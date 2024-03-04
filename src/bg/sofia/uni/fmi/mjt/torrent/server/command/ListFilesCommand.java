package bg.sofia.uni.fmi.mjt.torrent.server.command;

import bg.sofia.uni.fmi.mjt.torrent.server.storage.UserStorage;

import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class ListFilesCommand extends Command {

    public ListFilesCommand(UserStorage storage, SocketChannel socketChannel, AtomicBoolean attachment) {
        super(null, storage, socketChannel, attachment);
    }

    @Override
    public void run() {
        sendResponse(storage.listFiles());
        returnChannelToSelector();
    }

}
