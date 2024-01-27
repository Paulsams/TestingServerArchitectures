package services.communication.files;

import services.communication.CommunicationService;
import services.communication.CommunicationServiceException;
import services.communication.MessageStatus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class ReadFromFileCommunicationService implements CommunicationService, AutoCloseable {
    private final BufferedReader fileStream;
    private final Iterator<String> lines;

    public ReadFromFileCommunicationService(File pathToFile) throws CommunicationServiceException {
        try {
            fileStream = Files.newBufferedReader(pathToFile.toPath());
            lines = fileStream.lines().iterator();
        } catch (IOException ex) {
            throw new CommunicationServiceException(ex);
        }
    }

    @Override
    public void println() {
        lines.next();
    }

    @Override
    public void println(Object message, MessageStatus messageStatus) {
        lines.next();
    }

    @Override
    public String readln() {
        return lines.next();
    }

    @Override
    public void close() throws Exception {
        fileStream.close();
    }
}
