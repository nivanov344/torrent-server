package bg.sofia.uni.fmi.mjt.torrent.server.user;

import java.util.Set;

public record User(String address, int port, Set<String> availableFiles) {

    public String ip() {
        String[] tokens = address.split(":");
        return tokens[0];
    }
}
