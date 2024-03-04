package bg.sofia.uni.fmi.mjt.torrent.client.server;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSender implements Runnable {

    private static final int BUFFER_SIZE = 4096;
    private String pathToFile;
    private Socket socket;

    public FileSender(String pathToFile, Socket socket) {
        this.pathToFile = pathToFile;
        this.socket = socket;
    }

    @Override
    public void run() {
        int bytes;
        try (FileInputStream fileInputStream = new FileInputStream(pathToFile);
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            dataOutputStream.writeLong(Files.size(Path.of(pathToFile)));

            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytes = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytes);
                dataOutputStream.flush();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
