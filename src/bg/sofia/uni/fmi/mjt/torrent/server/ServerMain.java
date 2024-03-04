package bg.sofia.uni.fmi.mjt.torrent.server;

import bg.sofia.uni.fmi.mjt.torrent.server.storage.InMemoryUserStorage;

import java.util.Scanner;

public class ServerMain {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int port;
        System.out.println("Please enter a port value: ");
        port = sc.nextInt();

        Server server = new Server(port, new InMemoryUserStorage());
        server.start();

        sc.close();
    }
}
