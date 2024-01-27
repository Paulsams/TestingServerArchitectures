package servers.asynchoronous;

import servers.HandlerRequests;
import servers.NIOUtils;
import servers.ServerArchitecture;
import servers.ServerArchitectureException;
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
    private ServerArchitectureException error;

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
    ) throws ServerArchitectureException {
        isRunning = true;

        int m = parameters.get(ParameterType.M).getValue();

        try (var socketChannel = AsynchronousServerSocketChannel.open()) {
            socketChannel.bind(inetAddress, BACKLOG);

            callbackInitialized.onInitialized();

            var clients = new ClientHolder[m];
            for (int i = 0; i < m; i++) {
                var clientSocket = socketChannel.accept().get();

                var client = new ClientHolder(
                    clientSocket, ByteBuffer.allocate(START_READ_BUFFER_SIZE_ON_SERVER_BY_CLIENT), this
                );
                clients[i] = client;

                clientSocket.read(client.getReadBuffer(), client, new ReadCompletionHandler(
                    tasksThreadPool, handlerRequests
                ));
            }

            synchronized (this) {
                while (isRunning) {
                    // Не придумал. больше, кроме как тут чекать
                    this.wait();

                    if (error != null)
                        throw error;
                }
            }

            tasksThreadPool.shutdownNow();
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new ServerArchitectureException("Main Loop failed", e);
        }
    }

    @Override
    public void stop() {
        notifyMe();
    }

    void acceptError(ServerArchitectureException error) {
        this.error = error;
        notifyMe();
    }

    private void notifyMe() {
        synchronized (this) {
            isRunning = false;
            this.notify();
        }
    }
}
