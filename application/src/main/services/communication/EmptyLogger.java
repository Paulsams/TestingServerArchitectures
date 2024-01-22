package services.communication;

import services.loggers.LoggerService;

public class EmptyLogger implements LoggerService {
    @Override
    public void log(Object any) { }

    @Override
    public void info(Object any) { }

    @Override
    public void important(Object any) { }

    @Override
    public void veryImportant(Object any) { }
}
