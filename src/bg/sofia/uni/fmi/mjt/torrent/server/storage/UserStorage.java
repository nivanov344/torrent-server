package bg.sofia.uni.fmi.mjt.torrent.server.storage;

import bg.sofia.uni.fmi.mjt.torrent.server.exception.FileDoesNotExistException;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.UnauthorizedAccessException;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.UserDoesNotExistsException;

public interface UserStorage {
    void registerNewFiles(String username, String ip, int port, String[] files) throws UnauthorizedAccessException;

    void unregisterFiles(String username, String ip, String[] files) throws UserDoesNotExistsException,
            UnauthorizedAccessException, FileDoesNotExistException;

    void deleteUser(String username) throws UserDoesNotExistsException;

    String getUserIP(String username) throws UserDoesNotExistsException;

    int getUserPort(String username) throws UserDoesNotExistsException;

    String listFiles();

    String getUsernamesMapping();

    boolean exists(String username);
}
