import services.ServiceLocator;
import services.communication.CommunicationService;
import services.communication.files.ReadFromFileCommunicationService;
import services.loggers.ConsoleLogger;
import services.loggers.LogStatus;
import services.loggers.LoggerService;
import services.metrics.CollectorMetricsService;
import services.metrics.CollectorMetricsServiceImpl;
import testing.algorithms.ReadInputDirectoryTesting;

import java.io.File;
import java.util.Objects;
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

        var collectionMetricsService = new CollectorMetricsServiceImpl();
        serviceLocator.register(CollectorMetricsService.class, collectionMetricsService);

        var testingAlgorithm = new ReadInputDirectoryTesting(serviceLocator);
        testingAlgorithm.startTesting(collectionMetricsService);
    }
}
