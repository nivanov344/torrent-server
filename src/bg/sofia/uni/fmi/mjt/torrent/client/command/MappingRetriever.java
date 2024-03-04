package bg.sofia.uni.fmi.mjt.torrent.client.command;

import bg.sofia.uni.fmi.mjt.torrent.client.exception.InputFailedException;
import bg.sofia.uni.fmi.mjt.torrent.client.exception.OutputFailedException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class MappingRetriever implements Runnable {

    private static final int BUFFER_SIZE = 4096;

    private static final Command GET_MAPPING_COMMAND = new Command(CommandSender.GET_MAPPING, new String[0]);

    private final Map<String, String> usersMapping;
    private final SocketChannel socketChannel;
    private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    public MappingRetriever(SocketChannel socketChannel, Map<String, String> usersMapping) {
        this.socketChannel = socketChannel;
        this.usersMapping = usersMapping;
    }

    @Override
    public void run() {
        try {
            sendRequest();
            String mapping = getServerResponse();
            //System.out.println("Mapping received: " + mapping);
            if (mapping.equals("empty")) {
                usersMapping.clear();
                return;
            }
            updateMapping(mapping);
        } catch (InputFailedException | OutputFailedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendRequest() throws OutputFailedException {
        CommandSender.sendCommandToServer(GET_MAPPING_COMMAND, socketChannel, buffer);
    }

    private String getServerResponse() throws InputFailedException {
        return CommandSender.getServerResponse(socketChannel, buffer);
    }

    private void updateMapping(String newMapping) {
        usersMapping.clear();
        String[] lines = newMapping.split("\n");
        for (String line : lines) {
            String[] tokens = line.split(" - ");
            usersMapping.put(tokens[0], tokens[1]);
        }
    }
}
