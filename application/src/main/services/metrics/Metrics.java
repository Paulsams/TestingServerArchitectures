package services.metrics;

import services.metrics.MetricType;

import java.util.Map;

public record Metrics(
    int changedConstant,
    Map<MetricType, Double> valuesInMs
) { }
