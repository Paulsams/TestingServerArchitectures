package services.metrics;

import testing.parameters.ParameterType;

import java.util.List;

public record GroupMetric (
    ParameterType parameterType,
    List<Metrics> metrics
) { }
