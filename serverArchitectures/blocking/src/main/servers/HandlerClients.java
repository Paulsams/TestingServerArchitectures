package servers;

import messages.Messages;
import services.ServiceLocator;
import services.loggers.LoggerService;
import services.metrics.CollectMetricContext;
import services.metrics.CollectorMetricsService;
import services.metrics.MetricType;
import utils.BlockingArchitectureUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HandlerClients {
    private record ClientData(
        int id, ExecutorService writeThreadPool,
        DataOutputStream outputStream, CountDownLatch countDownLatch
    ) { }
    private record ClientDataWithMetric(ClientData client, CollectMetricContext metricContext) { }

    private final ExecutorService tasksThreadPool;
    private final LoggerService logger = ServiceLocator.get(LoggerService.class);
    private final CollectorMetricsService collectorMetrics = ServiceLocator.get(CollectorMetricsService.class);
    private final HandlerRequests<ClientDataWithMetric> handlerRequests = new HandlerRequests<>(this::sendResponse);

    public HandlerClients(ExecutorService tasksThreadPool) {
        this.tasksThreadPool = tasksThreadPool;
    }

    public void handleClient(Socket clientSocket, int clientId, int x) {
        var writeThreadPool = Executors.newSingleThreadExecutor();

        try {
            var inputStream = new DataInputStream(clientSocket.getInputStream());
            var outputStream = new DataOutputStream(clientSocket.getOutputStream());

            var clientData = new ClientData(
                clientId, writeThreadPool, outputStream, new CountDownLatch(x)
            );

            for (int i = 0; i < x; i++) {
                var metricContext = collectorMetrics.start(MetricType.HANDLE_CLIENT_TIME);
                var request = Messages.ArrayRequest.parseFrom(BlockingArchitectureUtils.readByteArrayFromStream(inputStream));
                logger.info("SERVER: Client " + clientId + " Read Request");

                tasksThreadPool.execute(() -> handlerRequests.handle(
                    new ClientDataWithMetric(clientData, metricContext),
                    request
                ));
            }

            clientData.countDownLatch().await();

            clientSocket.close();
            clientData.writeThreadPool.shutdownNow();
        } catch (IOException | InterruptedException ex) {
            throw new ServerException("Read Loop failed", ex);
        }
    }

    private void sendResponse(ClientDataWithMetric clientWithMetric, Messages.ArrayResponse response) {
        var client = clientWithMetric.client();

        client.writeThreadPool().execute(() -> {
            try {
                var responseInByteArray = response.toByteArray();
                clientWithMetric.metricContext().tryStop();
                client.outputStream.writeInt(response.getSerializedSize());
                client.outputStream.write(responseInByteArray);

                logger.info("SERVER: Sent Response to Client " + client.id);
                client.countDownLatch().countDown();
            } catch (IOException ex) {
                throw new ServerException("Client failed on write", ex);
            }
        });
    }
}
