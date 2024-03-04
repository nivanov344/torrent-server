package bg.sofia.uni.fmi.mjt.torrent.server.storage;

import bg.sofia.uni.fmi.mjt.torrent.server.exception.FileDoesNotExistException;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.UnauthorizedAccessException;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.UserDoesNotExistsException;
import bg.sofia.uni.fmi.mjt.torrent.server.user.User;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserStorage implements UserStorage {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private static final int MAX_PORT_VALUE = 65535;

    /**
     * Registers the given files from the user. If no user with this username exists, creates a new user with the given
     * username and associates it with the given address and client-side server port. The user modification operation is
     * performed synchronously.
     * @param username the username of the existing user or the username to be associated with the new user
     * @param clientAddress the address of the user, sending the register request
     * @param clientServerPort the port of the client-side server of the user
     * @param files the files which are to be registered for the given user
     * @throws IllegalArgumentException if the given username, IP, or files are null or the port value is invalid
     * @throws UnauthorizedAccessException if trying to register files with username, associated with another IP
     */
    @Override
    public void registerNewFiles(String username, String clientAddress, int clientServerPort, String[] files)
            throws UnauthorizedAccessException {
        if (username == null || clientAddress == null) {
            throw new IllegalArgumentException("Cannot register user with username or IP value null");
        }

        if (files == null) {
            throw new IllegalArgumentException("Cannot register user with files value null");
        }

        if (clientServerPort < 0 || clientServerPort > MAX_PORT_VALUE) {
            throw new IllegalArgumentException("Port value must be between 0 and 65535 but was: " + clientServerPort);
        }

        User user = users.get(username);
        if (user != null) {
            if (!clientAddress.equals(user.address()) || clientServerPort != user.port()) {
                throw new UnauthorizedAccessException("User and sender IPs or ports do not match\n" +
                        "Expected IP: " + user.address() +
                        "Actual IP: " + clientAddress );
            }
        } else {
            user = new User(clientAddress, clientServerPort, new HashSet<>(List.of(files)));
            users.put(username, user);
        }

        synchronized (user) {
            addFiles(user, files);
        }
    }

    /**
     * Unregisters the given files from the user. The user modification operation is performed synchronously.
     * @param username the username of the user, from which the files will be unregistered
     * @param clientAddress the address of the user, sending the unregister request
     * @param files the files which are to be unregistered for the given user
     * @throws IllegalArgumentException if the given username, IP, or files are null
     * @throws UserDoesNotExistsException if no user with the given username exists
     * @throws FileDoesNotExistException if a file is not registered with the given user
     * @throws UnauthorizedAccessException if the username of the user is associated with different IP of that
     * of the request sender
     */
    @Override
    public void unregisterFiles(String username, String clientAddress, String[] files)
            throws UserDoesNotExistsException, FileDoesNotExistException, UnauthorizedAccessException {
        if (username == null || clientAddress == null || files == null) {
            throw new IllegalArgumentException("Cannot unregister files when username or files are null");
        }

        User user = users.get(username);
        if (user != null) {
            if (!clientAddress.equals(user.address())) {
                throw new UnauthorizedAccessException("User and sender IPs or ports do not match\n" +
                        "Expected IP: " + user.address() +
                        "Actual IP: " + clientAddress );
            }
        } else {
            throw new UserDoesNotExistsException("User " + username + " does not exist");
        }

        synchronized (user) {
            removeFiles(users.get(username), files);
        }
    }

    @Override
    public void deleteUser(String username) throws UserDoesNotExistsException {
        if (username == null) {
            throw new IllegalArgumentException("Cannot delete user with username null");
        }

        if (users.remove(username) == null) {
            throw new UserDoesNotExistsException("User " + username + " does not exist");
        }
    }

    @Override
    public String getUserIP(String username) throws UserDoesNotExistsException {
        if (username == null) {
            throw new IllegalArgumentException("Cannot get user IP with username null");
        }

        try {
            return users.get(username).address();
        } catch (NullPointerException e) {
            throw new UserDoesNotExistsException("User " + username + " does not exist");
        }
    }

    @Override
    public int getUserPort(String username) throws UserDoesNotExistsException {
        if (username == null) {
            throw new IllegalArgumentException("Cannot get user port with username null");
        }

        try {
            return users.get(username).port();
        } catch (NullPointerException e) {
            throw new UserDoesNotExistsException("User " + username + " does not exist");
        }
    }

    @Override
    public String listFiles() {
        StringBuilder listedFiles = new StringBuilder();

        for (Map.Entry<String, User> entry : users.entrySet()) {
            Set<String> userFilepaths = entry.getValue().availableFiles();
            for (String userFilepath : userFilepaths) {
                listedFiles.append(entry.getKey()).append(" : ").append(userFilepath).append("\n");
            }
        }

        return listedFiles.toString();
    }

    @Override
    public String getUsernamesMapping() {
        StringBuilder usernamesMapping = new StringBuilder();

        for (Map.Entry<String, User> entry : users.entrySet()) {
            usernamesMapping.append(entry.getKey()).append(" - ").append(entry.getValue().ip())
                    .append(":").append(entry.getValue().port()).append("\n");
        }

        return usernamesMapping.toString();
    }

    @Override
    public boolean exists(String username) {
        return users.containsKey(username);
    }

    private void addFiles(User user, String[] files) {
        Set<String> userFiles = user.availableFiles();
        synchronized (userFiles) {
            Collections.addAll(userFiles, files);
        }
    }

    private void removeFiles(User user, String[] files) throws FileDoesNotExistException {
        Set<String> userFiles = user.availableFiles();
        synchronized (userFiles) {
            for (String file : files) {
                if (!userFiles.remove(file)) {
                    throw new FileDoesNotExistException("File does not exist for the given user: " + file);
                }
            }
        }
    }
}
