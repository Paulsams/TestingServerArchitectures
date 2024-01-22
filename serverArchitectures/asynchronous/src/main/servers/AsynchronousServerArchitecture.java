package servers;

import com.google.protobuf.InvalidProtocolBufferException;
import messages.Messages;
import services.ServiceLocator;
import services.metrics.CollectMetricContext;
import services.metrics.CollectorMetricsService;
import services.metrics.MetricType;
import testing.parameters.Parameter;
import testing.parameters.ParameterType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static servers.ServerConstants.*;

public class AsynchronousServerArchitecture implements ServerArchitecture {
    private record ClientDataWithMetric(ClientHolder clientHolder, CollectMetricContext metricContext) {
    }

    private static class ClientHolder extends ClientOnServer {
        public final AsynchronousSocketChannel socketChannel;

        protected ClientHolder(AsynchronousSocketChannel socketChannel, ByteBuffer readBuffer) {
            super(readBuffer);
            this.socketChannel = socketChannel;
        }
    }

    private final ExecutorService tasksThreadPool = Executors.newFixedThreadPool(SIZE_THREAD_POOL_FOR_TASKS);
    private final HandlerRequests<ClientDataWithMetric> handlerRequests;

    private final CollectorMetricsService collectorMetrics = ServiceLocator.get(CollectorMetricsService.class);

    private boolean isRunning;

    public AsynchronousServerArchitecture() {
        handlerRequests = new HandlerRequests<>((clientDataWithMetric, response) -> {
            clientDataWithMetric.metricContext.stop();

            NIOUtils.sendResponse(clientDataWithMetric.clientHolder, response,
                (client, writeBuffer) -> client.socketChannel.write(writeBuffer, client, new CompletionHandler<>() {
                    @Override
                    public void completed(Integer result, ClientHolder attachment) {
                        client.socketChannel.write(writeBuffer);
                        if (writeBuffer.hasRemaining())
                            client.socketChannel.write(client.getReadBuffer(), client, this);
                    }

                    @Override
                    public void failed(Throwable exc, ClientHolder attachment) {
                        throw new RuntimeException(exc);
                    }
                })
            );
        });
    }

    @Override
    public void start(
        InetSocketAddress inetAddress,
        Map<ParameterType, Parameter> parameters,
        OnServerInitialized callbackInitialized
    ) throws IOException {
        isRunning = true;

        int m = parameters.get(ParameterType.M).getValue();

        try (var socketChannel = AsynchronousServerSocketChannel.open()) {
            socketChannel.bind(inetAddress, BACKLOG);

            callbackInitialized.onInitialized();
            ;
            for (int i = 0; i < m; i++) {
                var clientSocket = socketChannel.accept().get();

                var client = new ClientHolder(
                    clientSocket, ByteBuffer.allocate(START_READ_BUFFER_SIZE_ON_SERVER_BY_CLIENT)
                );
                clientSocket.read(client.getReadBuffer(), client, new CompletionHandler<>() {
                    @Override
                    public void completed(Integer result, ClientHolder attachment) {
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

                        clientSocket.read(client.getReadBuffer(), client, this);
                    }

                    @Override
                    public void failed(Throwable exc, ClientHolder attachment) {
                        throw new RuntimeException(exc);
                    }
                });
            }

            synchronized (this) {
                while (isRunning) {
                    this.wait();
                }
            }

            tasksThreadPool.shutdownNow();
        } catch (ExecutionException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            isRunning = false;
            this.notify();
        }
    }
}
