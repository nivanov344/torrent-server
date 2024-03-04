package bg.sofia.uni.fmi.mjt.torrent.server.command;

import bg.sofia.uni.fmi.mjt.torrent.server.storage.UserStorage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Command implements Runnable {

    protected static final int BUFFER_INITIAL_CAPACITY = 8192;

    protected String[] arguments;
    protected UserStorage storage;
    protected SocketChannel socketChannel;
    protected ByteBuffer buffer = ByteBuffer.allocate(BUFFER_INITIAL_CAPACITY);
    protected AtomicBoolean attachment;

    public Command(String[] arguments, UserStorage storage, SocketChannel socketChannel, AtomicBoolean attachment) {
        this.arguments = arguments;
        this.storage = storage;
        this.socketChannel = socketChannel;
        this.attachment = attachment;
    }

    protected void sendResponse(String response) {
        try {
            buffer.clear();
            buffer.put(response.getBytes());
            buffer.flip();

            socketChannel.write(buffer);
        } catch (IOException e) {
            System.out.println("Response sending failed");
        }

    }

    protected String getFilesInLines(String[] files) {
        StringBuilder sb = new StringBuilder();
        for (String file : files) {
            sb.append(file).append("\n");
        }

        return sb.toString();
    }

    protected void returnChannelToSelector() {
        attachment.compareAndSet(false, true);
    }
}
