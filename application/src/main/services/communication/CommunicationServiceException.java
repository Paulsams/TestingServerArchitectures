package services.communication;

public class CommunicationServiceException extends Exception {
    private final static String MESSAGE = "Failed Communication Service";

    public CommunicationServiceException(String message) {
        super(MESSAGE + ": \n" + message);
    }

    public CommunicationServiceException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
