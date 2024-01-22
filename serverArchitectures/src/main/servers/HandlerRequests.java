package servers;

import com.google.protobuf.InvalidProtocolBufferException;
import messages.Messages;
import services.ServiceLocator;
import services.metrics.CollectorMetricsService;
import services.metrics.MetricType;

import java.util.ArrayList;

class HandlerRequests<TClient> {
    @FunctionalInterface
    public interface SenderRequest<TClient> {
        void sendRequest(TClient client, Messages.ArrayResponse response);
    }

    private final SenderRequest<TClient> senderRequest;
    private static final CollectorMetricsService collectorMetrics = ServiceLocator.get(CollectorMetricsService.class);

    HandlerRequests(SenderRequest<TClient> senderRequest) {
        this.senderRequest = senderRequest;
    }

    public void handle(
        TClient client,
        Messages.ArrayRequest arrayRequest
    ) {
        var sortedArray = sortArray(arrayRequest);

        var arrayResponse = Messages.ArrayResponse.newBuilder().addAllSortedArray(sortedArray).build();
        senderRequest.sendRequest(client, arrayResponse);
    }

    private static ArrayList<Integer> sortArray(Messages.ArrayRequest arrayRequest) {
        var metricContext = collectorMetrics.start(MetricType.SORTED_TIME);

        var sortedArray = new ArrayList<>(arrayRequest.getArrayList());

        final int len = sortedArray.size();
        for (int i = 0; i < len - 1; i++) {
            for (int j = 0; j < len - i - 1; j++) {
                var left = sortedArray.get(j);
                var right = sortedArray.get(j + 1);

                if (left > right) {
                    int tmp = left;
                    sortedArray.set(j, right);
                    sortedArray.set(j + 1, tmp);
                }
            }
        }

        metricContext.stop();
        return sortedArray;
    }
}
