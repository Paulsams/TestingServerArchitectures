package testing.algorithms;

import charts.CreatorChart;
import servers.asynchoronous.AsynchronousServerArchitecture;
import servers.BlockingServerArchitecture;
import servers.NonBlockingServerArchitecture;
import servers.ServerArchitecture;
import services.ServiceLocator;
import services.communication.CommunicationService;
import services.communication.files.ReadFromFileCommunicationService;
import services.loggers.LoggerService;
import services.metrics.CollectorMetricsServiceImpl;
import services.metrics.MetricType;
import services.metrics.GroupMetric;
import testing.MainLoop;
import services.metrics.Metrics;
import testing.TestingConfiguration;
import testing.WaiterTestingConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

import static charts.MyChartUtils.RESULTS_DIR;

public class ReadInputDirectoryTesting implements ITestingAlgorithm {
    private final ServiceLocator serviceLocator;

    private final LoggerService logger = ServiceLocator.get(LoggerService.class);

    public ReadInputDirectoryTesting(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public void startTesting(CollectorMetricsServiceImpl collectionMetricsService) {
        for (var inputFile : Objects.requireNonNull(new File("INPUT").listFiles())) {
            if (inputFile.isDirectory())
                continue;

            serviceLocator.register(CommunicationService.class, () ->
                new ReadFromFileCommunicationService(inputFile)
            );

            startOneLoopTest(collectionMetricsService);
        }
    }

    private void startOneLoopTest(CollectorMetricsServiceImpl collectionMetricsService) {
        var configuration = new WaiterTestingConfiguration().get();

        Supplier<ServerArchitecture> architectureFactory = () -> switch (configuration.architectureType()) {
            case BLOCKING -> new BlockingServerArchitecture();
            case NON_BLOCKING -> new NonBlockingServerArchitecture();
            case ASYNCHRONOUS -> new AsynchronousServerArchitecture();
        };

        var updateParameter = configuration.parameters().updateParameter();

        var allMetrics = new ArrayList<Metrics>();
        do {
            var metrics = startTest(collectionMetricsService, architectureFactory, configuration);
            allMetrics.add(metrics);

            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
            for (var metricType : MetricType.values()) {
                new CreatorChart(
                    configuration.architectureType(), groupMetric, metricType
                ).drawAndSave(pathToDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Metrics startTest(
        CollectorMetricsServiceImpl collectionMetricsService,
        Supplier<ServerArchitecture> architectureFactory,
        TestingConfiguration configuration
    ) {
        var parameters = configuration.parameters();
        var updateParameter = parameters.updateParameter();

        logger.veryImportant("Start Testing: Arch - " + configuration.architectureType() +
            " and changed parameter - " + updateParameter.getType() + " have value: " + updateParameter.getValue());
        var mainLoop = new MainLoop(parameters);
        mainLoop.startTesting(architectureFactory.get());

        var metrics = collectionMetricsService.collect(updateParameter.getValue(), parameters.all());
        logger.veryImportant("Stop Testing");
        return metrics;
    }
}
