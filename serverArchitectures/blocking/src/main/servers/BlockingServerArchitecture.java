package servers;

import messages.Messages;
import services.ServiceLocator;
import services.loggers.LoggerService;
import services.metrics.CollectMetricContext;
import services.metrics.CollectorMetricsService;
import services.metrics.MetricType;
import testing.parameters.Parameter;
import testing.parameters.ParameterType;
import utils.BlockingArchitectureUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static servers.ServerConstants.BACKLOG;
import static servers.ServerConstants.SIZE_THREAD_POOL_FOR_TASKS;

public class BlockingServerArchitecture implements ServerArchitecture {
    private record ClientData(
        int id, ExecutorService writeThreadPool,
        DataOutputStream outputStream, CountDownLatch countDownLatch
    ) {
    }

    private record ClientDataWithMetric(ClientData client, CollectMetricContext metricContext) { }

    private final ExecutorService tasksThreadPool = Executors.newFixedThreadPool(SIZE_THREAD_POOL_FOR_TASKS);
    private final HandlerRequests<ClientDataWithMetric> handlerRequests = new HandlerRequests<>(this::sendResponse);
    private final LoggerService logger = ServiceLocator.get(LoggerService.class);
    private final CollectorMetricsService collectorMetrics = ServiceLocator.get(CollectorMetricsService.class);

    private Boolean isRunning;

    @Override
    public void start(
        InetSocketAddress inetAddress,
        Map<ParameterType, Parameter> parameters,
        OnServerInitialized callbackInitialized
    ) throws IOException {
        isRunning = true;

        try (var socket = new ServerSocket(inetAddress.getPort(), BACKLOG, inetAddress.getAddress())) {
            int m = parameters.get(ParameterType.M).getValue();
            int x = parameters.get(ParameterType.X).getValue();

            callbackInitialized.onInitialized();
            logger.info("SERVER: started");

            for (int i = 0; i < m; i++) {
                var clientSocket = socket.accept();
                logger.log("Client " + i + " was accepted by the server");

                int finalI = i;
                new Thread(() -> executeClient(clientSocket, finalI, x)).start();
            }

            synchronized (this) {
                while (isRunning) {
                    this.wait();
                }
            }

            tasksThreadPool.shutdownNow();
            logger.info("SERVER: stopped");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            isRunning = false;
            this.notify();
        }
    }

    private void executeClient(Socket clientSocket, int clientId, int x) {
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
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendResponse(ClientDataWithMetric clientWithMetric, Messages.ArrayResponse response) {
        var client = clientWithMetric.client();

        client.writeThreadPool().execute(() -> {
            try {
                clientWithMetric.metricContext().tryStop();
                client.outputStream.writeInt(response.getSerializedSize());
                response.writeTo(client.outputStream);

                logger.info("SERVER: Sent Response to Client " + client.id);
                client.countDownLatch().countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
