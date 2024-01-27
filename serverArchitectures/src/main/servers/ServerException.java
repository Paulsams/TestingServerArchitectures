package servers;

public class ServerException extends RuntimeException {
    private static final String MESSAGE_NAME = "message";
    private static final String CAUSE_NAME = "message";

    public ServerException(String message) {
        super(getPrefixMessage(MESSAGE_NAME) + message);
    }

    public ServerException(String message, Throwable cause) {
        super(getPrefixMessage(MESSAGE_NAME) + message + "\n and " + CAUSE_NAME + " :\n", cause);
    }

    public ServerException(Throwable cause) {
        super(getPrefixMessage(CAUSE_NAME), cause);
    }

    private static String getPrefixMessage(String causeName) {
        return "Strange behavior was detected on the server side with the " + causeName + ": \n";
    }
}
