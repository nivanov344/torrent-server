package bg.sofia.uni.fmi.mjt.torrent.server.exception;

public class UnknownCommandException extends Exception {
    public UnknownCommandException() {
    }

    public UnknownCommandException(String message) {
        super(message);
    }

    public UnknownCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownCommandException(Throwable cause) {
        super(cause);
    }

    public UnknownCommandException(String message, Throwable cause,
                                   boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
