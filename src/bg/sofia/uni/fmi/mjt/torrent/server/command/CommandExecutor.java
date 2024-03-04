package bg.sofia.uni.fmi.mjt.torrent.server.command;

import bg.sofia.uni.fmi.mjt.torrent.server.exception.UnknownCommandException;
import bg.sofia.uni.fmi.mjt.torrent.server.storage.UserStorage;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandExecutor {

    private CommandCreator commandCreator;
    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private Map<SocketChannel, String> channelsToUsernames;

    public CommandExecutor(UserStorage storage, Map<SocketChannel, String> channelsToUsernames) {
        commandCreator = new CommandCreator(storage);
        this.channelsToUsernames = channelsToUsernames;
    }

    public void execute(String userInput, SocketChannel socketChannel, AtomicBoolean attachment) {
        try {
            Runnable command = commandCreator.newCommand(userInput, socketChannel, attachment, channelsToUsernames);
            executor.execute(command);
        } catch (UnknownCommandException e) {
            System.out.println(e.getMessage());
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

}
