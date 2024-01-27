package services.communication.files;

import services.communication.CommunicationService;
import services.communication.CommunicationServiceException;
import services.communication.MessageStatus;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class WriteToFileCommunicationService implements CommunicationService, AutoCloseable {
    private final PrintStream fileStream;
    private final String[] exampleAnswers;
    private int currentAnswer = 0;

    public WriteToFileCommunicationService(String pathToFile, String[] exampleAnswers) throws CommunicationServiceException {
        try {
            fileStream = new PrintStream(pathToFile);
        } catch (FileNotFoundException ex) {
            throw new CommunicationServiceException(ex);
        }

        this.exampleAnswers = exampleAnswers;
    }

    @Override
    public void println() {
        fileStream.println();
    }

    @Override
    public void println(Object message, MessageStatus messageStatus) {
        fileStream.println(message);
    }

    @Override
    public String readln() {
        var answer = exampleAnswers[currentAnswer++];
        fileStream.println(answer);
        return answer;
    }

    @Override
    public void close() {
        fileStream.close();
    }
}
