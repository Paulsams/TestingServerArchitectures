import services.ServiceLocator;
import services.loggers.ConsoleLogger;
import services.loggers.LogStatus;
import services.loggers.LoggerService;
import testing.algorithms.ReadInputDirectoryTesting;

import java.util.Set;

public class EntryPoint {
    public static void main(String[] args) {
        var serviceLocator = new ServiceLocator();

        var logger =
            new ConsoleLogger(Set.of(
                LogStatus.LOG,
                LogStatus.INFO,
                LogStatus.IMPORTANT
            ));
        serviceLocator.register(LoggerService.class, logger);

        var testingAlgorithm = new ReadInputDirectoryTesting(serviceLocator);
        testingAlgorithm.startTesting();
    }
}
