package services.metrics;

import testing.parameters.Parameter;
import testing.parameters.ParameterType;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollectorMetricsServiceImpl implements CollectorMetricsService {
    private class MetricHolder implements CollectMetricContext {
        public final Temporal startTime;
        public final MetricType metricType;

        private boolean isUsed;

        private MetricHolder(Temporal startTime, MetricType metricType) {
            this.startTime = startTime;
            this.metricType = metricType;
        }

        @Override
        public boolean tryStop() {
            if (isUsed)
                return false;

            CollectorMetricsServiceImpl.this.stop(this);
            isUsed = true;
            return true;
        }
    }

    private static class EmptyMetricContext implements CollectMetricContext {
        @Override
        public boolean tryStop() {
            return true;
        }
    }

    private final Map<MetricType, ConcurrentLinkedQueue<Long>> metrics = Collections.unmodifiableMap(
        new EnumMap<>(Arrays.stream(MetricType.values())
            .collect(Collectors.toMap(
                Function.identity(),
                type -> new ConcurrentLinkedQueue<>()
            ))
        ));

    private final AtomicInteger _countClientsNotStartMetric;
    private final int _countClients;

    public CollectorMetricsServiceImpl(Map<ParameterType, Parameter> parameters) {
        _countClients = parameters.get(ParameterType.M).getValue();
        _countClientsNotStartMetric = new AtomicInteger(_countClients);
    }

    @Override
    public CollectMetricContext start(MetricType metricType) {
        if (_countClientsNotStartMetric.get() == 0 || (
            metricType == MetricType.CLIENT_FULL_LOOP_TIME &&
                _countClientsNotStartMetric.updateAndGet((it) -> Math.max(it - 1, 0)) == 0)
        ) {
            return new MetricHolder(Instant.now(), metricType);
        }

        return new EmptyMetricContext();
    }

    private void stop(MetricHolder metricHolder) {
        var metricTime = Duration.between(metricHolder.startTime, Instant.now()).toMillis();

        if (metricHolder.metricType == MetricType.CLIENT_FULL_LOOP_TIME) {
            _countClientsNotStartMetric.compareAndSet(0, _countClients);
            metrics.get(metricHolder.metricType).add(metricTime);
        }

        if (_countClientsNotStartMetric.get() == 0)
            metrics.get(metricHolder.metricType).add(metricTime);
    }

    public Metrics collect(int parameter, Map<ParameterType, Parameter> parameters) {
        var x = parameters.get(ParameterType.X).getValue();
        var delta = parameters.get(ParameterType.DELTA).getValue();

        var allSortedTime = metrics.get(MetricType.SORTED_TIME);
        var allHandleClientTime = metrics.get(MetricType.HANDLE_CLIENT_TIME);
        var allClientFullLoop = metrics.get(MetricType.CLIENT_FULL_LOOP_TIME);

        var sortedTime = allSortedTime.stream()
            .skip((long) (allSortedTime.size() * 0.2)).limit((long) Math.ceil(allSortedTime.size() * 0.8))
            .mapToDouble(Long::doubleValue).average().getAsDouble();
        var handleClientTime = allHandleClientTime.stream()
            .skip((long) (allHandleClientTime.size() * 0.2)).limit((long) Math.ceil(allHandleClientTime.size() * 0.8))
            .mapToDouble(Long::doubleValue).average().getAsDouble();
        var clientFullLoop = allClientFullLoop.stream()
            .skip((long) (allClientFullLoop.size() * 0.2)).limit((long) Math.ceil(allClientFullLoop.size() * 0.8))
            .mapToDouble(number -> (number.doubleValue() - delta * x) / x).average().getAsDouble();

        allSortedTime.clear();
        allHandleClientTime.clear();
        allClientFullLoop.clear();

        return new Metrics(
            parameter,
            new EnumMap<>(Map.of(
                MetricType.SORTED_TIME, sortedTime,
                MetricType.HANDLE_CLIENT_TIME, handleClientTime,
                MetricType.CLIENT_FULL_LOOP_TIME, clientFullLoop
            ))
        );
    }
}
