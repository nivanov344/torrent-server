package bg.sofia.uni.fmi.mjt.torrent.client.exception;

public class InvalidArgumentListException extends Exception {
    public InvalidArgumentListException() {
    }

    public InvalidArgumentListException(String message) {
        super(message);
    }

    public InvalidArgumentListException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidArgumentListException(Throwable cause) {
        super(cause);
    }

    public InvalidArgumentListException(String message, Throwable cause,
                                        boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
