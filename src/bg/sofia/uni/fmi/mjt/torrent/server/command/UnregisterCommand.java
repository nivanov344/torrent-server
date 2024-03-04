package bg.sofia.uni.fmi.mjt.torrent.server.command;

import bg.sofia.uni.fmi.mjt.torrent.server.exception.FileDoesNotExistException;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.UnauthorizedAccessException;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.UserDoesNotExistsException;
import bg.sofia.uni.fmi.mjt.torrent.server.storage.UserStorage;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class UnregisterCommand extends Command {

    private static final int USERNAME_POS = 0;
    private static final int FILES_FROM_POS = 1;

    public UnregisterCommand(String[] arguments, UserStorage storage,
                             SocketChannel socketChannel, AtomicBoolean attachment) {
        super(arguments, storage, socketChannel, attachment);
    }

    @Override
    public void run() {
        String response;
        String username = arguments[USERNAME_POS];
        String[] files = Arrays.copyOfRange(arguments, FILES_FROM_POS, arguments.length);

        try {
            String address = socketChannel.getRemoteAddress().toString().substring(1);
            System.out.println("register request received from " + address);
            storage.unregisterFiles(username, address, files);
            response = "Successfully unregistered " + files.length + " files:\n" + getFilesInLines(files);
        } catch (UnauthorizedAccessException e) {
            response = "You are not authorized to manipulate files of user " + username;
        } catch (FileDoesNotExistException e) {
            response = e.getMessage();
        } catch (UserDoesNotExistsException e) {
            response = "No user with username " + username + " exists";
        } catch (IOException e) {
            response = "Network error occurred while trying to update files of user " + username +
                    ". Please try again later";
            System.out.println("IO error occurred while retrieving client address");
        }

        sendResponse(response);
        returnChannelToSelector();
    }
}
