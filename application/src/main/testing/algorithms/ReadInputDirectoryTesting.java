package testing.algorithms;

import charts.CreatorChart;
import servers.BlockingServerArchitecture;
import servers.NonBlockingServerArchitecture;
import servers.ServerArchitecture;
import servers.asynchoronous.AsynchronousServerArchitecture;
import services.ServiceLocator;
import services.communication.CommunicationService;
import services.communication.files.ReadFromFileCommunicationService;
import services.loggers.LoggerService;
import services.metrics.*;
import testing.MainLoop;
import testing.TestingConfiguration;
import testing.TestingException;
import testing.WaiterTestingConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

import static charts.MyChartUtils.RESULTS_DIR;

public class ReadInputDirectoryTesting implements ITestingAlgorithm {
    private static final int DELAY_FOR_GC_IN_MS = 2000;

    private final ServiceLocator serviceLocator;

    private final LoggerService logger = ServiceLocator.get(LoggerService.class);

    public ReadInputDirectoryTesting(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public void startTesting() {
        for (var inputFile : Objects.requireNonNull(new File("INPUT").listFiles())) {
            if (inputFile.isDirectory())
                continue;

            try {
                serviceLocator.register(CommunicationService.class, new ReadFromFileCommunicationService(inputFile));
                startOneLoopTest();
            } catch (Throwable ex) {
                throw new TestingException(ex);
            }
        }
    }

    private void startOneLoopTest() throws InterruptedException {
        var configuration = new WaiterTestingConfiguration().get();

        Supplier<ServerArchitecture> architectureFactory = () -> switch (configuration.architectureType()) {
            case BLOCKING -> new BlockingServerArchitecture();
            case NON_BLOCKING -> new NonBlockingServerArchitecture();
            case ASYNCHRONOUS -> new AsynchronousServerArchitecture();
        };

        var updateParameter = configuration.parameters().updateParameter();

        var allMetrics = new ArrayList<Metrics>();
        do {
            {
                var collectionMetricsService = new CollectorMetricsServiceImpl(configuration.parameters().all());
                serviceLocator.register(CollectorMetricsService.class, collectionMetricsService);

                var metrics = startTest(collectionMetricsService, architectureFactory, configuration);
                allMetrics.add(metrics);

                serviceLocator.unregister(CollectorMetricsService.class);
            }

            System.gc();
            Thread.sleep(DELAY_FOR_GC_IN_MS);
        } while (updateParameter.update());

        var groupMetric = new GroupMetric(updateParameter.getType(), allMetrics);

        var pathToDirectory = Path.of(
            RESULTS_DIR,
            configuration.architectureType().toString(),
            updateParameter.getType().toString()
        ).toFile();

        if (!pathToDirectory.exists() && !pathToDirectory.mkdirs())
            throw new TestingException("Creation of folders for saving results failed with an error");

        try {
            for (var metricType : MetricType.values()) {
                new CreatorChart(
                    configuration.architectureType(), groupMetric, metricType
                ).drawAndSave(pathToDirectory);
            }
        } catch (IOException ex) {
            throw new TestingException("An error occurred while saving the chart", ex);
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
