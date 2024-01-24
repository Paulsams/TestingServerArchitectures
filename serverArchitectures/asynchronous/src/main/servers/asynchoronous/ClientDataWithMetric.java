package servers.asynchoronous;

import services.metrics.CollectMetricContext;

record ClientDataWithMetric(ClientHolder clientHolder, CollectMetricContext metricContext) { }
