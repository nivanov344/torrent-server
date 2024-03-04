package bg.sofia.uni.fmi.mjt.torrent.client.server;

import bg.sofia.uni.fmi.mjt.torrent.client.command.Command;
import bg.sofia.uni.fmi.mjt.torrent.client.command.CommandCreator;
import bg.sofia.uni.fmi.mjt.torrent.client.command.CommandSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientServer extends Thread {

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private Set<String> availableFiles = new HashSet<>();
    private CommandCreator commandCreator;
    private boolean isServerWorking;

    public ClientServer(int maxSenderThreads) {
        try {
            serverSocket = new ServerSocket(0);
            executor = Executors.newFixedThreadPool(maxSenderThreads);
            isServerWorking = true;
            commandCreator = new CommandCreator(getServerPort());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void run() {
        Socket clientSocket;

        try {
            while (isServerWorking) {
                clientSocket = serverSocket.accept();
                clientSocket.setKeepAlive(true);

                System.out.println(clientSocket.isClosed());
                processRequest(clientSocket);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            executor.shutdownNow();
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public void shutdown() {
        isServerWorking = false;
    }

    private void processRequest(Socket clientSocket) {
        String inputLine;
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            inputLine = in.readLine();
            Command cmd = commandCreator.newCommand(inputLine);
            String[] args = cmd.arguments();
            String pathToFile = args[0];

            if (!cmd.command().equals(CommandSender.REQUEST_PERMISSION) || args.length != 1) {
                throw new RuntimeException();
            } else if (!availableFiles.contains(pathToFile)) {
                out.println(CommandSender.PERMISSION_DENIED + " " + CommandCreator.formatArgument(pathToFile));
            } else {
                out.println(CommandSender.PERMISSION_GRANTED + " " + CommandCreator.formatArgument(pathToFile));
                executor.execute(new FileSender(pathToFile, clientSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeCommand(Command cmd) {
        switch (cmd.command()) {
            case CommandSender.REGISTER -> registerFiles(cmd);
            case CommandSender.UNREGISTER -> unregisterFiles(cmd);
            default -> { }
        }
    }

    public int getServerPort() {
        return serverSocket.getLocalPort();
    }

    private void registerFiles(Command command) {
        if (command.arguments().length <= 1) {
            throw new RuntimeException("Please enter username and at least one file to register");
        }

        String[] files = Arrays.copyOfRange(command.arguments(), 2, command.arguments().length);
        availableFiles.addAll(Arrays.asList(files));
    }

    private void unregisterFiles(Command command) {
        if (command.arguments().length <= 1) {
            throw new RuntimeException("Please enter username and at least one file to register");
        }

        String[] files = Arrays.copyOfRange(command.arguments(), 1, command.arguments().length);
        Arrays.asList(files).forEach(availableFiles::remove);
    }

}
