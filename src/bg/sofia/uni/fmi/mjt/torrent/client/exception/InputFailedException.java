package bg.sofia.uni.fmi.mjt.torrent.client.exception;

import java.io.IOException;

public class InputFailedException extends IOException {
    public InputFailedException() {
    }

    public InputFailedException(String message) {
        super(message);
    }

    public InputFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public InputFailedException(Throwable cause) {
        super(cause);
    }
}