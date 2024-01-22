package services.loggers;

import com.diogonunes.jcolor.Attribute;

import java.util.Set;

import static com.diogonunes.jcolor.Ansi.colorize;

public class ConsoleLogger implements LoggerService {
    private final Set<LogStatus> notAllowedStatuses;

    public ConsoleLogger(Set<LogStatus> notAllowedStatuses) {
        this.notAllowedStatuses = notAllowedStatuses;
    }

    @Override
    public void log(Object any) {
        if (notAllowedStatuses.contains(LogStatus.LOG))
            return;

        System.out.println(colorize(any.toString(), Attribute.WHITE_TEXT()));
    }

    @Override
    public void info(Object any) {
        if (notAllowedStatuses.contains(LogStatus.INFO))
            return;

        System.out.println(colorize(any.toString(), Attribute.YELLOW_TEXT()));
    }

    @Override
    public void important(Object any) {
        if (notAllowedStatuses.contains(LogStatus.IMPORTANT))
            return;

        System.out.println(colorize(any.toString(), Attribute.GREEN_TEXT()));
    }

    @Override
    public void veryImportant(Object any) {
        if (notAllowedStatuses.contains(LogStatus.VERY_IMPORTANT))
            return;

        System.out.println(colorize(any.toString(), Attribute.BLUE_TEXT()));
    }
}

