package bg.sofia.uni.fmi.mjt.torrent.server.exception;

public class FileDoesNotExistException extends Exception {
    public FileDoesNotExistException() {
    }

    public FileDoesNotExistException(String message) {
        super(message);
    }

    public FileDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileDoesNotExistException(Throwable cause) {
        super(cause);
    }

    public FileDoesNotExistException(String message, Throwable cause,
                                     boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
