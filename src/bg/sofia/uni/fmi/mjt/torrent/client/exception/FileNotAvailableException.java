package bg.sofia.uni.fmi.mjt.torrent.client.exception;

public class FileNotAvailableException extends Exception {
    public FileNotAvailableException() {
    }

    public FileNotAvailableException(String message) {
        super(message);
    }

    public FileNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileNotAvailableException(Throwable cause) {
        super(cause);
    }

    public FileNotAvailableException(String message, Throwable cause,
                                     boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
