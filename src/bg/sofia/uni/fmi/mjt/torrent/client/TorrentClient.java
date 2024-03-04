package bg.sofia.uni.fmi.mjt.torrent.client;

import bg.sofia.uni.fmi.mjt.torrent.client.command.Command;
import bg.sofia.uni.fmi.mjt.torrent.client.command.CommandCreator;
import bg.sofia.uni.fmi.mjt.torrent.client.command.CommandSender;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.FileNotAvailableException;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.InvalidArgumentListException;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.UserDoesNotExistException;
import bg.sofia.uni.fmi.mjt.torrent.client.server.ClientServer;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.FileDoesNotExistException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class TorrentClient {

    private static final int BUFFER_INITIAL_CAPACITY = 8192;
    private static final int MAX_CLIENT_SERVER_THREADS = 10;
    private static final String DISCONNECT = "disconnect";

    private CommandCreator commandCreator;
    private CommandSender commandSender;
    private ClientServer clientServer;
    private Scanner userInput;
    private SocketChannel socketChannel;
    private ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_INITIAL_CAPACITY);

    public TorrentClient(String serverAddress, int serverPort, Scanner userInput) {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(serverAddress, serverPort));

            System.out.println("Connected to the server");

            clientServer = new ClientServer(MAX_CLIENT_SERVER_THREADS);

            commandCreator = new CommandCreator(clientServer.getServerPort());

            commandSender = new CommandSender(socketChannel, buffer, clientServer);

            this.userInput = userInput;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void start() {
        clientServer.start();

        while (true) {
            String input = userInput.nextLine();

            if (input.equals(DISCONNECT)) {
                break;
            }

            Command command = commandCreator.newCommand(input);

            try {
                sendCommand(command);
            } catch (FileDoesNotExistException | IOException | FileNotAvailableException |
                     InvalidArgumentListException | UserDoesNotExistException e) {
                System.out.println(e.getMessage());
            }
        }

        disconnect();
    }

    private void sendCommand(Command command) throws FileDoesNotExistException, FileNotAvailableException,
            InvalidArgumentListException, IOException, UserDoesNotExistException {
        System.out.println(commandSender.send(command));
    }

    public void disconnect() {
        try {
            clientServer.shutdown();
            commandSender.shutdown();
            socketChannel.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
