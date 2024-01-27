package servers;

import services.ServiceLocator;
import services.loggers.LoggerService;
import testing.parameters.Parameter;
import testing.parameters.ParameterType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.ServerException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static servers.ServerConstants.BACKLOG;
import static servers.ServerConstants.SIZE_THREAD_POOL_FOR_TASKS;

public class BlockingServerArchitecture implements ServerArchitecture {
    private final ExecutorService tasksThreadPool = Executors.newFixedThreadPool(SIZE_THREAD_POOL_FOR_TASKS);
    private final LoggerService logger = ServiceLocator.get(LoggerService.class);
    private final HandlerClients handlerClients = new HandlerClients(tasksThreadPool);

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
                new Thread(() -> handlerClients.handleClient(clientSocket, finalI, x)).start();
            }

            synchronized (this) {
                while (isRunning) {
                    this.wait();
                }
            }

            tasksThreadPool.shutdownNow();
            logger.info("SERVER: stopped");
        } catch (InterruptedException ex) {
            throw new ServerException("Main Loop failed", ex);
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
