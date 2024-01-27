package testing;

import java.io.IOException;

public class TestingException extends RuntimeException {
    public TestingException(String message) {
        super(message);
    }

    public TestingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestingException(Throwable cause) {
        super(cause);
    }
}
