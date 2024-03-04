package bg.sofia.uni.fmi.mjt.torrent.server.exception;

public class InvalidCommandFormatException extends Exception {
    public InvalidCommandFormatException() {
    }

    public InvalidCommandFormatException(String message) {
        super(message);
    }

    public InvalidCommandFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCommandFormatException(Throwable cause) {
        super(cause);
    }

    public InvalidCommandFormatException(String message, Throwable cause,
                                         boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
