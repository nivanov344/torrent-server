package bg.sofia.uni.fmi.mjt.torrent.server.command;

import bg.sofia.uni.fmi.mjt.torrent.server.storage.UserStorage;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class GetMappingCommand extends Command {

    public GetMappingCommand(UserStorage storage, SocketChannel socketChannel, AtomicBoolean attachment) {
        super(null, storage, socketChannel, attachment);
    }

    @Override
    public void run() {
        String mapping = storage.getUsernamesMapping();
        if (mapping.isBlank()) {
            mapping = "empty";
        }
        sendResponse(mapping);
        System.out.println(mapping);
        try {
            System.out.println("Mapping successfully sent to " + socketChannel.getRemoteAddress());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        returnChannelToSelector();
    }
}
