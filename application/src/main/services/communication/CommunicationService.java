package services.communication;

import services.Service;

public interface CommunicationService extends Service {
    void println();
    void println(Object message, MessageStatus messageStatus);

    String readln();
}