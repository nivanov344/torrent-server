package bg.sofia.uni.fmi.mjt.torrent.client;

import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String serverAddress;
        int serverPort;
        System.out.println("Please enter server address and port:");
        serverAddress = scanner.nextLine();
        serverPort = scanner.nextInt();

        TorrentClient torrentClient = new TorrentClient(serverAddress, serverPort, scanner);
        torrentClient.start();
    }
}
