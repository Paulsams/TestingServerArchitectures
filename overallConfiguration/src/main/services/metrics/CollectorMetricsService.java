package services.metrics;

import services.Service;

public interface CollectorMetricsService extends Service {
    CollectMetricContext start(MetricType metricType);
}
