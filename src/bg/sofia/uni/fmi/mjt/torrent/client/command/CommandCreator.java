package bg.sofia.uni.fmi.mjt.torrent.client.command;

import java.util.ArrayList;
import java.util.List;

public class CommandCreator {

    private int clientServerPort;

    public CommandCreator(int clientServerPort) {
        this.clientServerPort = clientServerPort;
    }

    private static List<String> getCommandArguments(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        boolean insideQuote = false;

        for (char c : input.toCharArray()) {
            if (c == '"') {
                insideQuote = !insideQuote;
            }
            if (c == ' ' && !insideQuote) {
                tokens.add(sb.toString().replace("\"", ""));
                sb.delete(0, sb.length());
            } else {
                sb.append(c);
            }
        }

        tokens.add(sb.toString().replace("\"", ""));

        return tokens;
    }

    public Command newCommand(String clientInput) {
        if (clientInput == null) {
            throw new IllegalArgumentException("Client input cannot be null");
        }
        List<String> tokens = CommandCreator.getCommandArguments(clientInput);
        if (tokens.getFirst().equals(CommandSender.REGISTER)) {
            tokens.add(2, String.valueOf(clientServerPort));
        }
        String[] args = tokens.subList(1, tokens.size()).toArray(new String[0]);

        return new Command(tokens.getFirst(), args);
    }

    public static String formatArgument(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (path.contains(" ")) {
            return  "\"" + path + "\"";
        }

        return path;
    }
}
