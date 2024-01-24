package servers.asynchoronous;

import com.google.protobuf.InvalidProtocolBufferException;
import messages.Messages;
import servers.HandlerRequests;
import servers.NIOUtils;
import services.ServiceLocator;
import services.metrics.CollectorMetricsService;
import services.metrics.MetricType;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;

class ReadCompletionHandler implements CompletionHandler<Integer, ClientHolder> {
    private final ExecutorService tasksThreadPool;
    private final CollectorMetricsService collectorMetrics = ServiceLocator.get(CollectorMetricsService.class);
    private final HandlerRequests<ClientDataWithMetric> handlerRequests;

    public ReadCompletionHandler(ExecutorService tasksThreadPool, HandlerRequests<ClientDataWithMetric> handlerRequests) {
        this.tasksThreadPool = tasksThreadPool;
        this.handlerRequests = handlerRequests;
    }

    @Override
    public void completed(Integer result, ClientHolder client) {
        if (result == -1)
            return;

        client.readCountBytes += result;
        if (client.getReadBuffer().capacity() <= client.readCountBytes)
            client.resize();

        if (NIOUtils.readyMessage(client)) {
            client.readCountBytes -= Integer.BYTES + client.currentBufferLength;
            byte[] messageByteArray = new byte[client.currentBufferLength];
            client.getReadBuffer().flip();
            client.getReadBuffer().get(messageByteArray);
            client.getReadBuffer().compact();

            try {
                var metricContext = collectorMetrics.start(MetricType.HANDLE_CLIENT_TIME);
                var arrayRequest = Messages.ArrayRequest.parseFrom(messageByteArray);

                tasksThreadPool.execute(() -> handlerRequests.handle(
                    new ClientDataWithMetric(client, metricContext), arrayRequest
                ));
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }

            client.currentBufferLength = null;
        }

        client.socketChannel.read(client.getReadBuffer(), client, this);
    }

    @Override
    public void failed(Throwable exc, ClientHolder attachment) {
        throw new RuntimeException(exc);
    }
}
