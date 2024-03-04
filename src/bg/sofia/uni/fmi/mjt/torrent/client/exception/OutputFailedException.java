package bg.sofia.uni.fmi.mjt.torrent.client.exception;

import java.io.IOException;

public class OutputFailedException extends IOException {
    public OutputFailedException() {
    }

    public OutputFailedException(String message) {
        super(message);
    }

    public OutputFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutputFailedException(Throwable cause) {
        super(cause);
    }
}
