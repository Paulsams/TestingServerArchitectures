package testing.algorithms;

import services.metrics.CollectorMetricsServiceImpl;

public interface ITestingAlgorithm {
    void startTesting(CollectorMetricsServiceImpl collectionMetricsService);
}
