package servers.asynchoronous;

import servers.HandlerRequests;
import servers.NIOUtils;
import servers.ServerArchitecture;
import testing.parameters.Parameter;
import testing.parameters.ParameterType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static servers.ServerConstants.*;

public class AsynchronousServerArchitecture implements ServerArchitecture {
    private final ExecutorService tasksThreadPool = Executors.newFixedThreadPool(SIZE_THREAD_POOL_FOR_TASKS);
    private final HandlerRequests<ClientDataWithMetric> handlerRequests;

    private boolean isRunning;

    public AsynchronousServerArchitecture() {
        handlerRequests = new HandlerRequests<>((clientDataWithMetric, response) ->
            NIOUtils.sendResponse(clientDataWithMetric.clientHolder(), response,
                (client, writeBuffer) -> client.socketChannel.write(
                    writeBuffer, clientDataWithMetric, new WriteCompletionHandler(writeBuffer)
                )
            )
        );
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
                clientSocket.read(client.getReadBuffer(), client, new ReadCompletionHandler(
                    tasksThreadPool, handlerRequests
                ));
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
