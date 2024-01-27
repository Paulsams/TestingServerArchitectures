package servers;

import services.ServiceLocator;
import services.loggers.LoggerService;
import testing.parameters.Parameter;
import testing.parameters.ParameterType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;

import static servers.ServerConstants.*;

public class NonBlockingServerArchitecture implements ServerArchitecture {
    private final LoggerService logger = ServiceLocator.get(LoggerService.class);
    private boolean isRunning;

    @Override
    public void start(
        InetSocketAddress inetAddress,
        Map<ParameterType, Parameter> parameters,
        OnServerInitialized callbackInitialized
    ) throws IOException {
        isRunning = true;

        int m = parameters.get(ParameterType.M).getValue();

        try (var serverSocketChannel = ServerSocketChannel.open();
             Selector readSelector = Selector.open();
             Selector writeSelector = Selector.open()
        ) {
            serverSocketChannel.socket().bind(inetAddress, BACKLOG);

            var writeLoop = createAndStartWriteLoop(writeSelector);
            var readLoop = createAndStartReadLoop(readSelector, writeLoop);

            logger.info("SERVER: started");
            callbackInitialized.onInitialized();
            for (int i = 0; i < m; i++) {
                SocketChannel clientSocketChannel = serverSocketChannel.accept();
                clientSocketChannel.configureBlocking(false);
                ClientHolder.Client client = new ClientHolder.Client(
                    ByteBuffer.allocate(START_READ_BUFFER_SIZE_ON_SERVER_BY_CLIENT)
                );
                readLoop.addClient(new ClientHolder(client, clientSocketChannel));
            }

            synchronized (this) {
                while (isRunning) {
                    this.wait();
                }
            }

            readLoop.close();

            logger.info("SERVER: stopped");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerException("Main Loop Server failed", e);
        }
    }

    private static ReadLoop createAndStartReadLoop(Selector readSelector, WriteLoop writeLoop) {
        var readLoop = new ReadLoop(readSelector, writeLoop::addClient);
        new Thread(() -> {
            try {
                readLoop.readClients();
            } catch (ClosedSelectorException ignored) {
            } catch (IOException e) {
                throw new ServerException("Read Loop failed", e);
            }
        }).start();
        return readLoop;
    }

    private static WriteLoop createAndStartWriteLoop(Selector writeSelector) {
        var writeLoop = new WriteLoop(writeSelector);
        new Thread(() -> {
            try {
                writeLoop.writeClients();
            } catch (ClosedSelectorException ignored) {
            } catch (IOException e) {
                throw new ServerException("Write Loop failed", e);
            }
        }).start();

        return writeLoop;
    }

    @Override
    public void stop() {
        synchronized (this) {
            isRunning = false;
            this.notify();
        }
    }
}
