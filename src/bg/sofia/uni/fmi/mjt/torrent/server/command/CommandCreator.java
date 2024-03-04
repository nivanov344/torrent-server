package bg.sofia.uni.fmi.mjt.torrent.server.command;

import bg.sofia.uni.fmi.mjt.torrent.server.exception.InvalidCommandFormatException;
import bg.sofia.uni.fmi.mjt.torrent.server.exception.UnknownCommandException;
import bg.sofia.uni.fmi.mjt.torrent.server.storage.UserStorage;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandCreator {

    public static final String REGISTER = "register";
    public static final String UNREGISTER = "unregister";
    public static final String LIST_FILES = "list-files";
    public static final String GET_MAPPING = "get-mapping";

    private static final int REGISTER_COMMAND_MIN_ARG_COUNT = 3;

    private UserStorage storage;

    public CommandCreator(UserStorage storage) {
        this.storage = storage;
    }

    public Command newCommand(String clientInput, SocketChannel socketChannel, AtomicBoolean attachment,
                              Map<SocketChannel, String> channelsToUsernames) throws UnknownCommandException {
        List<String> tokens = CommandCreator.getCommandArguments(clientInput);
        String[] args = tokens.subList(1, tokens.size()).toArray(new String[0]);
        String cmd = tokens.getFirst();

        try {
            return switch(cmd) {
                case REGISTER -> register(args, socketChannel, attachment, channelsToUsernames);
                case UNREGISTER -> unregister(args, socketChannel, attachment);
                case LIST_FILES -> list(args, socketChannel, attachment);
                case GET_MAPPING -> getMapping(args, socketChannel, attachment);
                default -> throw new UnknownCommandException("Unknown command: " + cmd);
            };
        } catch (InvalidCommandFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public static String formatArgument(String path) {
        if (path.contains(" ")) {
            return  "\"" + path + "\"";
        }

        return path;
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

    private Command register(String[] arguments, SocketChannel socketChannel, AtomicBoolean attachment,
                             Map<SocketChannel, String> channelsToUsernames) throws InvalidCommandFormatException {
        if (arguments.length < REGISTER_COMMAND_MIN_ARG_COUNT) {
            throw new InvalidCommandFormatException(
                    "Register command must have at least 3 arguments but were actually: " + arguments.length);
        }

        for (String argument : arguments) {
            if (argument == null) {
                throw new InvalidCommandFormatException("No null values expected in register command argument list");
            }
        }

        return new RegisterCommand(arguments, storage, socketChannel, attachment, channelsToUsernames);
    }

    private Command unregister(String[] arguments, SocketChannel socketChannel, AtomicBoolean attachment)
            throws InvalidCommandFormatException {
        if (arguments.length < 2) {
            throw new InvalidCommandFormatException(
                    "Unregister command must have at least 2 arguments but were actually: " + arguments.length);
        }

        for (String argument : arguments) {
            if (argument == null) {
                throw new InvalidCommandFormatException("No null values expected in unregister command argument list");
            }
        }

        return new UnregisterCommand(arguments, storage, socketChannel, attachment);
    }

    private Command list(String[] arguments, SocketChannel socketChannel, AtomicBoolean attachment)
            throws InvalidCommandFormatException {
        if (arguments.length != 0) {
            throw new InvalidCommandFormatException(
                    "No arguments expected in list command but were actually: " + arguments.length);
        }

        return new ListFilesCommand(storage, socketChannel, attachment);
    }

    private Command getMapping(String[] arguments, SocketChannel socketChannel, AtomicBoolean attachment)
            throws InvalidCommandFormatException {
        if (arguments.length != 0) {
            throw new InvalidCommandFormatException(
                    "No arguments expected in get-mapping command but were actually: " + arguments.length);
        }

        return new GetMappingCommand(storage, socketChannel, attachment);
    }
}
