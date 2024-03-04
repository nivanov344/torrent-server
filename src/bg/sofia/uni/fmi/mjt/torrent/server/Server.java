package bg.sofia.uni.fmi.mjt.torrent.server;

import bg.sofia.uni.fmi.mjt.torrent.server.command.CommandExecutor;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.UserDoesNotExistsException;
import bg.sofia.uni.fmi.mjt.torrent.server.storage.UserStorage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private static final int BUFFER_SIZE = 1024;
    private static final String HOST = "localhost";

    private ByteBuffer buffer;
    private Selector selector;
    private UserStorage storage;
    private CommandExecutor commandExecutor;
    private final int port;
    private final Map<SocketChannel, String> channelsToUsernames = new ConcurrentHashMap<>();

    private boolean isServerWorking;

    public Server(int port, UserStorage storage) {
        this.port = port;
        this.storage = storage;
        commandExecutor = new CommandExecutor(storage, channelsToUsernames);
    }

    public void start() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            selector = Selector.open();
            configureServerSocketChannel(serverSocketChannel, selector);
            this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
            isServerWorking = true;
            System.out.println("Server started and listening on " + serverSocketChannel.getLocalAddress().toString());

            while (isServerWorking) {
                SocketChannel clientChannel = null;

                try {
                    int readyChannels = selector.select();
                    if (readyChannels == 0) {
                        continue;
                    }

                    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        if (key.isReadable()) {
                            AtomicBoolean attachment = (AtomicBoolean) key.attachment();
                            if (!attachment.get()) {
                                continue;
                            }

                            clientChannel = (SocketChannel) key.channel();
                            String clientInput = getClientInput(clientChannel);
                            System.out.println(clientInput);
                            if (clientInput == null) {
                                continue;
                            }

                            if (attachment.compareAndSet(true, false)) {
                                commandExecutor.execute(clientInput, clientChannel, attachment);
                            }
                        } else if (key.isAcceptable()) {
                            accept(selector, key);
                        }

                        keyIterator.remove();
                    }

                } catch (IOException e) {
                    System.out.println("Error occurred while processing client request: " + e.getMessage());
                    clearInactiveUser(clientChannel);
                }
            }

            commandExecutor.shutdown();
        } catch (IOException | UserDoesNotExistsException e) {
            throw new RuntimeException("failed");
        }
    }

    public void stop() {
        this.isServerWorking = false;
        if (selector.isOpen()) {
            selector.wakeup();
        }
    }

    private void clearInactiveUser(SocketChannel clientChannel) throws IOException, UserDoesNotExistsException {
        if (clientChannel != null) {
            String username = channelsToUsernames.get(clientChannel);
            if (username != null) {
                storage.deleteUser(username);
            }
            channelsToUsernames.remove(clientChannel);
            clientChannel.close();
        }
    }

    private void configureServerSocketChannel(ServerSocketChannel channel, Selector selector) throws IOException {
        channel.bind(new InetSocketAddress(HOST, this.port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private String getClientInput(SocketChannel clientChannel) throws IOException {
        buffer.clear();

        int readBytes = clientChannel.read(buffer);
        if (readBytes < 0) {
            clientChannel.close();
            return null;
        }

        buffer.flip();

        byte[] clientInputBytes = new byte[buffer.remaining()];
        buffer.get(clientInputBytes);

        return new String(clientInputBytes, StandardCharsets.UTF_8);
    }

    private void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel sockChannel = (ServerSocketChannel) key.channel();
        SocketChannel accept = sockChannel.accept();

        accept.configureBlocking(false);
        accept.register(selector, SelectionKey.OP_READ);
        accept.keyFor(selector).attach(new AtomicBoolean(true));
    }
}
