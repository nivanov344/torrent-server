package bg.sofia.uni.fmi.mjt.torrent.client.command;

import bg.sofia.uni.fmi.mjt.torrent.client.download.FileDownloader;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.FileNotAvailableException;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.InvalidArgumentListException;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.InputFailedException;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.OutputFailedException;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.UserDoesNotExistException;
import bg.sofia.uni.fmi.mjt.torrent.client.server.ClientServer;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.FileDoesNotExistException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandSender {

    public static final String REGISTER = "register";
    public static final String UNREGISTER = "unregister";
    public static final String LIST_FILES = "list-files";
    public static final String DOWNLOAD = "download";
    public static final String GET_MAPPING = "get-mapping";
    public static final String REQUEST_PERMISSION = "request-permission";
    public static final String PERMISSION_GRANTED = "permission-granted";
    public static final String PERMISSION_DENIED = "permission-denied";

    private static final int USER_DOWNLOAD_POS = 0;
    private static final int PATH_TO_FILE_POS = 1;
    private static final int PATH_TO_SAVE_POS = 2;

    private static final int USER_IP_POS = 0;
    private static final int USER_PORT_POS = 1;

    private static final int USERNAME_POS = 0;

    private static final int REQUEST_INTERVAL = 30_000;
    private static final int DOWNLOAD_COMMAND_ARG_COUNT = 3;
    private static double bufferExpansion = 2;

    private SocketChannel socketChannel;
    private ByteBuffer buffer;
    private Map<String, String> usersMapping = new HashMap<>();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private ClientServer clientServer;
    private String username;

    public CommandSender(SocketChannel socketChannel, ByteBuffer buffer, ClientServer clientServer) {
        this.socketChannel = socketChannel;
        this.buffer = buffer;
        this.clientServer = clientServer;
        scheduleMappings();
    }

    public String send(Command cmd) throws IOException, FileDoesNotExistException, InvalidArgumentListException,
            FileNotAvailableException, UserDoesNotExistException {
        return switch (cmd.command()) {
            case REGISTER -> register(cmd);
            case UNREGISTER -> unregister(cmd);
            case DOWNLOAD -> download(cmd);
            case LIST_FILES -> list(cmd);
            default -> "Unknown command";
        };
    }

    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        clientServer.shutdown();
    }

    public void setBufferExpansionCoefficient(double coefficient) {
        if (coefficient < 1) {
            throw new IllegalArgumentException("Cannot set buffer expansion coefficient to less than 1");
        }
        bufferExpansion = coefficient;
    }

    public int getServerPort() {
        return clientServer.getServerPort();
    }

    public String getUsername() {
        return username;
    }

    private void scheduleMappings() {
        executorService.scheduleAtFixedRate(new MappingRetriever(socketChannel, usersMapping),
                0, REQUEST_INTERVAL, TimeUnit.MILLISECONDS);

    }

    private void requestMappingImmediately() {
        synchronized (usersMapping) {
            executorService.submit(new MappingRetriever(socketChannel, usersMapping));
        }
    }

    private String register(Command command) throws FileDoesNotExistException, InvalidArgumentListException,
            OutputFailedException, InputFailedException {
        if (command.arguments().length <= 1) {
            throw new InvalidArgumentListException("Please enter username and at least one file to register");
        }

        String[] files = Arrays.copyOfRange(command.arguments(), 2, command.arguments().length);
        for (String file : files) {
            if (!Files.exists(Path.of(file))) {
                throw new FileDoesNotExistException("File " + file + " does not exist");
            }
        }

        username = command.arguments()[USERNAME_POS];
        clientServer.executeCommand(command);
        sendCommandToServer(command, socketChannel, buffer);
        return getServerResponse(socketChannel, buffer);
    }

    private String unregister(Command command) throws InputFailedException,
            OutputFailedException, InvalidArgumentListException {
        if (command.arguments().length <= 1) {
            throw new InvalidArgumentListException("Please enter username and at least one file to unregister");
        }

        clientServer.executeCommand(command);
        sendCommandToServer(command, socketChannel, buffer);
        return getServerResponse(socketChannel, buffer);
    }

    private String list(Command command) throws InvalidArgumentListException,
            OutputFailedException, InputFailedException {
        if (command.arguments().length != 0) {
            throw new InvalidArgumentListException("No parameters expected to the list command");
        }

        sendCommandToServer(command, socketChannel, buffer);
        return getServerResponse(socketChannel, buffer);
    }

    private String download(Command cmd) throws InvalidArgumentListException,
            FileNotAvailableException, IOException, UserDoesNotExistException {
        String[] args = cmd.arguments();
        if (cmd.arguments().length != DOWNLOAD_COMMAND_ARG_COUNT) {
            throw new InvalidArgumentListException("Download command should have username, " +
                    "path to file and path to save as parameters");
        }

        return startDownload(args[USER_DOWNLOAD_POS], args[PATH_TO_FILE_POS], args[PATH_TO_SAVE_POS]);
    }

    private static String assembleCommand(Command command) {
        StringBuilder cmd = new StringBuilder(command.command());

        for (String argument : command.arguments()) {
            String formattedArg = CommandCreator.formatArgument(argument);
            cmd.append(" ").append(formattedArg);
        }

        return cmd.toString();
    }

    private static void putCommandInBuffer(Command command, ByteBuffer buffer) {
        buffer.clear();
        byte[] commandBytes = assembleCommand(command).getBytes();

        if (buffer.capacity() < commandBytes.length) {
            buffer = ByteBuffer.allocateDirect((int) (bufferExpansion * commandBytes.length));
            buffer.put(commandBytes);
        } else {
            buffer.put(commandBytes);
        }
    }

    protected static void sendCommandToServer(Command command, SocketChannel socketChannel, ByteBuffer buffer)
            throws OutputFailedException {
        putCommandInBuffer(command, buffer);
        buffer.flip();
        try {
            socketChannel.write(buffer);
        } catch (IOException e) {
            throw new OutputFailedException("An error occurred while trying to send "
                    + command.command() + " command with parameters "
                    + Arrays.toString(command.arguments()), e);
        }
    }

    protected static String getServerResponse(SocketChannel socketChannel, ByteBuffer buffer)
            throws InputFailedException {
        buffer.clear();
        try {
            socketChannel.read(buffer);
        } catch (IOException e) {
            throw new InputFailedException("An error occurred while trying to read response from server", e);
        }
        buffer.flip();

        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        return new String(byteArray, StandardCharsets.UTF_8);
    }

    private String startDownload(String username, String pathToFile, String pathToSave)
            throws IOException, FileNotAvailableException, UserDoesNotExistException {
        Socket socket = new Socket(getUserIp(username), getUserPort(username));
        if (isFileAvailableForDownload(socket, pathToFile)) {
            Thread fileDownloader = new Thread(new FileDownloader(socket, pathToFile, pathToSave, this));
            fileDownloader.start();
        } else {
            throw new FileNotAvailableException("File " + pathToFile + " is not available for download from user "
                    + username +
                    ". Please make sure you have entered the username and filepath correctly");
        }

        return "Started downloading file " + pathToFile + "from user " + username;
    }

    private boolean isFileAvailableForDownload(Socket socket, String pathToFile) throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String formattedPath = CommandCreator.formatArgument(pathToFile);

        writer.println(REQUEST_PERMISSION + " " + formattedPath);
        String reply = reader.readLine();
        if (reply.equals(PERMISSION_GRANTED + " " + formattedPath)) {
            return true;
        } else if (reply.equals(PERMISSION_DENIED + " " + formattedPath)) {
            return false;
        }

        throw new IOException("A problem occurred while requesting permission for file download of " + formattedPath);
    }

    private String getUserIp(String username) throws UserDoesNotExistException {
        String userData = getUserData(username);

        String[] tokens = userData.split(":");
        return tokens[USER_IP_POS];
    }

    private int getUserPort(String username) throws UserDoesNotExistException {
        String userData = getUserData(username);

        String[] tokens = userData.split(":");
        return Integer.parseInt(tokens[USER_PORT_POS]);
    }

    private String getUserData(String username) throws UserDoesNotExistException {
        String userData = usersMapping.get(username);
        if (userData == null) {
            requestMappingImmediately();
            synchronized (usersMapping) {
                userData = usersMapping.get(username);
            }
        }

        if (userData == null) {
            throw new UserDoesNotExistException("No user with username " + username +
                    " could be found. Please make sure that the username is correct.");
        }

        return userData;
    }
}
