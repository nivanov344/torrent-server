package bg.sofia.uni.fmi.mjt.torrent.client.download;

import bg.sofia.uni.fmi.mjt.torrent.client.command.Command;
import bg.sofia.uni.fmi.mjt.torrent.client.command.CommandSender;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.FileNotAvailableException;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.InvalidArgumentListException;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.UserDoesNotExistException;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.FileDoesNotExistException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class FileDownloader implements Runnable {

    private static final int BUFFER_SIZE = 4096;

    private final Socket socket;
    private final String pathToFile;
    private final String pathToSave;
    private CommandSender commandSender;

    public FileDownloader(Socket socket, String pathToFile, String pathToSave, CommandSender commandSender) {
        this.socket = socket;
        this.pathToFile = pathToFile;
        this.pathToSave = pathToSave;
        this.commandSender = commandSender;
    }

    @Override
    public void run() {
        try (DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {
            File file = new File(pathToSave);
            try (FileOutputStream fileOutputStream = new FileOutputStream(file, false)) {
                int bytes;
                long size = dataInputStream.readLong();
                byte[] buffer = new byte[BUFFER_SIZE];
                while (size > 0 &&
                        (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
                    fileOutputStream.write(buffer, 0, bytes);
                    size -= bytes;
                }
            }
            socket.close();
        } catch (IOException e) {
            System.out.println("A problem occurred while trying to download " + pathToFile + ". " +
                    "It is possible that the owner of the file has disconnected.\nError: " + e.getMessage());
            return;
        }
        System.out.println("File " + pathToFile + " successfully downloaded to " + pathToSave);
        try {
            commandSender.send(new Command("register", new String[]{commandSender.getUsername(),
                    String.valueOf(commandSender.getServerPort()), pathToSave}));
        } catch (IOException | FileDoesNotExistException | InvalidArgumentListException | FileNotAvailableException |
                 UserDoesNotExistException e) {
            throw new RuntimeException(e);
        }
    }
}
