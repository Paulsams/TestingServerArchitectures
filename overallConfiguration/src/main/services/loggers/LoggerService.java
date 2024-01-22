package services.loggers;

import services.Service;

public interface LoggerService extends Service {
    void log(Object any);
    void info(Object any);
    void important(Object any);
    void veryImportant(Object any);
}
