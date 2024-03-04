package bg.sofia.uni.fmi.mjt.torrent.server.command;

import bg.sofia.uni.fmi.mjt.torrent.server.exception.UnauthorizedAccessException;
import bg.sofia.uni.fmi.mjt.torrent.server.storage.UserStorage;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegisterCommand extends Command {

    private static final int USERNAME_POS = 0;
    private static final int PORT_POS = 1;
    private static final int FILES_FROM_POS = 2;

    private Map<SocketChannel, String> channelsToUsernames;

    public RegisterCommand(String[] arguments, UserStorage storage, SocketChannel socketChannel,
                           AtomicBoolean attachment, Map<SocketChannel, String> channelsToUsernames) {
        super(arguments, storage, socketChannel, attachment);
        this.channelsToUsernames = channelsToUsernames;
    }

    @Override
    public void run() {
        String response;
        String username = arguments[USERNAME_POS];
        int port = Integer.parseInt(arguments[PORT_POS]);
        String[] files = Arrays.copyOfRange(arguments, FILES_FROM_POS, arguments.length);

        try {
            String address = socketChannel.getRemoteAddress().toString().substring(1);
            storage.registerNewFiles(username, address, port, files);
            channelsToUsernames.put(socketChannel, username);
            response = "Successfully registered " + files.length + " files:\n" + getFilesInLines(files);
        } catch (UnauthorizedAccessException e) {
            response = "You are not authorized to manipulate files of user " + username;
        } catch (IOException e) {
            response = "Network error occurred while trying to update files of user " + username +
                    ". Please try again later";
            System.out.println("IO error occurred while retrieving client address");
        }

        sendResponse(response);
        returnChannelToSelector();
    }

}
