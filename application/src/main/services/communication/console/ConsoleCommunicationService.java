package services.communication.console;

import services.communication.CommunicationService;
import services.communication.MessageStatus;

import java.util.Scanner;
import java.util.Set;

import static com.diogonunes.jcolor.Ansi.colorize;
import static com.diogonunes.jcolor.Attribute.*;

public class ConsoleCommunicationService implements CommunicationService {
    private final Set<MessageStatus> notAllowedStatuses;
    private Scanner inputScanner = new Scanner(System.in);

    public ConsoleCommunicationService(Set<MessageStatus> notAllowedStatuses) {
        this.notAllowedStatuses = notAllowedStatuses;
    }

    @Override
    public void println() {
        System.out.println();
    }

    @Override
    public void println(Object message, MessageStatus messageStatus) {
        if (notAllowedStatuses.contains(messageStatus))
            return;

        System.out.println(getColorizedText(message.toString(), messageStatus));
    }

    @Override
    public String readln() {
        return inputScanner.nextLine();
    }

    private String getColorizedText(String text, MessageStatus messageStatus) {
        return colorize(text, switch (messageStatus) {
            case QUESTION -> BRIGHT_GREEN_TEXT();
        });
    }
}
