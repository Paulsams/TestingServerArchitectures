import servers.AsynchronousServerArchitecture;
import servers.BlockingServerArchitecture;
import servers.NonBlockingServerArchitecture;
import servers.ServerArchitecture;
import services.ServiceLocator;
import services.communication.CommunicationService;
import services.communication.files.ReadFromFileCommunicationService;
import services.loggers.ConsoleLogger;
import services.loggers.LogStatus;
import services.loggers.LoggerService;
import services.metrics.CollectorMetricsService;
import services.metrics.CollectorMetricsServiceImpl;
import services.metrics.MetricType;
import charts.CreatorChart;
import testing.GroupMetric;
import testing.Metrics;
import testing.WaiterTestingConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static charts.MyChartUtils.RESULTS_DIR;

public class EntryPoint {
    public static void main(String[] args) {
        var serviceLocator = new ServiceLocator();

        var logger =
            new ConsoleLogger(Set.of(
                LogStatus.LOG,
                LogStatus.INFO,
                LogStatus.IMPORTANT
            ));
//            new EmptyLogger();
        serviceLocator.register(LoggerService.class,
            () -> logger
        );

        var collectionMetricsService = new CollectorMetricsServiceImpl();
        serviceLocator.register(CollectorMetricsService.class, () -> collectionMetricsService);

        for (var inputFile : Objects.requireNonNull(new File("INPUT").listFiles())) {
            if (inputFile.isDirectory())
                continue;

            serviceLocator.register(CommunicationService.class, () ->
                new ReadFromFileCommunicationService(inputFile)
            );

            startTesting(collectionMetricsService);
        }
    }

    private static void startTesting(CollectorMetricsServiceImpl collectionMetricsService) {
        var logger = ServiceLocator.get(LoggerService.class);

        var configuration = new WaiterTestingConfiguration().get();

        Supplier<ServerArchitecture> architectureFactory = () -> switch (configuration.architectureType()) {
            case BLOCKING -> new BlockingServerArchitecture();
            case NON_BLOCKING -> new NonBlockingServerArchitecture();
            case ASYNCHRONOUS -> new AsynchronousServerArchitecture();
        };

        var parameters = configuration.parameters();
        var updateParameter = parameters.updateParameter();

        var allMetrics = new ArrayList<Metrics>();
        do {
            logger.veryImportant("Start Testing: Arch - " + configuration.architectureType() +
                " and changed parameter - " + updateParameter.getType() + " have value: " + updateParameter.getValue());
            var mainLoop = new MainLoop(parameters);
            mainLoop.startTesting(architectureFactory.get());

            allMetrics.add(collectionMetricsService.collect(updateParameter.getValue(), parameters.all()));
            logger.veryImportant("Stop Testing");
        } while (updateParameter.update());

        var groupMetric = new GroupMetric(updateParameter.getType(), allMetrics);

        var pathToDirectory = Path.of(
            RESULTS_DIR,
            configuration.architectureType().toString(),
            updateParameter.getType().toString()
        ).toFile();

        if (!pathToDirectory.exists() && !pathToDirectory.mkdirs())
            throw new RuntimeException("Creation of folders for saving results failed with an error");

        try {
            new CreatorChart(
                configuration.architectureType(), groupMetric, MetricType.SORTED_TIME
            ).drawAndSave(pathToDirectory);

            new CreatorChart(
                configuration.architectureType(), groupMetric, MetricType.HANDLE_CLIENT_TIME
            ).drawAndSave(pathToDirectory);

            new CreatorChart(
                configuration.architectureType(), groupMetric, MetricType.CLIENT_FULL_LOOP_TIME
            ).drawAndSave(pathToDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
