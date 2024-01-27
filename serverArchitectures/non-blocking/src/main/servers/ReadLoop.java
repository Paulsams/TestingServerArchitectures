package servers;

import messages.Messages;
import services.ServiceLocator;
import services.loggers.LoggerService;
import services.metrics.CollectMetricContext;
import services.metrics.CollectorMetricsService;
import services.metrics.MetricType;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static servers.ServerConstants.SIZE_THREAD_POOL_FOR_TASKS;

class ReadLoop implements AutoCloseable {
    private record ClientDataWithMetric(ClientHolder clientHolder, CollectMetricContext metricContext) { }

    private final Queue<ClientHolder> waitingReadingClients = new ConcurrentLinkedQueue<>();

    private final Selector readSelector;

    private final ExecutorService tasksThreadPool = Executors.newFixedThreadPool(SIZE_THREAD_POOL_FOR_TASKS);
    private final HandlerRequests<ClientDataWithMetric> handlerRequests;
    private final NIOUtils.OnSendResponse<ClientDataWithMetric> callbackOnSendClientResponse;

    private final LoggerService logger = ServiceLocator.get(LoggerService.class);
    private final CollectorMetricsService collectorMetrics = ServiceLocator.get(CollectorMetricsService.class);

    public ReadLoop(Selector readSelector, Consumer<ClientHolder> callbackClientReadyFromWrite) {
        this.readSelector = readSelector;
        this.callbackOnSendClientResponse = (clientWithMetric, buffer) -> {
            clientWithMetric.clientHolder.client().waitingBuffers.add(
                new ClientHolder.WriteBufferWithMetricContext(buffer, clientWithMetric.metricContext())
            );
            callbackClientReadyFromWrite.accept(clientWithMetric.clientHolder);
        };
        handlerRequests = new HandlerRequests<>((client, response) ->
            NIOUtils.sendResponse(client, response, callbackOnSendClientResponse));
    }

    public void addClient(ClientHolder clientHolder) {
        waitingReadingClients.add(clientHolder);
        readSelector.wakeup();
    }

    public void readClients() throws IOException {
        while (true) {
            int countSelected = readSelector.select();
            logger.log("SERVER: Read select return " + countSelected);

            // Processing
            Set<SelectionKey> selectedKeys = readSelector.selectedKeys();
            Iterator<SelectionKey> keysIterator = selectedKeys.iterator();
            while (keysIterator.hasNext()) {
                SelectionKey key = keysIterator.next();
                readHandleClient(key);
                keysIterator.remove();
            }

            registerNewClients();
        }
    }

    private void registerNewClients() throws ClosedChannelException {
        while (!waitingReadingClients.isEmpty()) {
            ClientHolder clientHolder = waitingReadingClients.poll();
            if (clientHolder == null)
                break;

            SelectionKey key = clientHolder.socketChannel().register(readSelector, SelectionKey.OP_READ);
            key.attach(clientHolder.client());

            logger.log("SERVER: Client: " +
                clientHolder.socketChannel().socket().getInetAddress() +
                " Registered from Read");
        }
    }

    private void readHandleClient(SelectionKey key) throws IOException {
        boolean clientIsCancel;
        try {
            clientIsCancel = !readClient(key);
        } catch (java.net.SocketException e) {
            clientIsCancel = true;
        }

        if (clientIsCancel) {
            var clientSocket = ((SocketChannel) key.channel()).socket();
            var remoteSocketAddress = clientSocket.getRemoteSocketAddress();
            logger.important("SERVER: Client: " + remoteSocketAddress + " cancel");

            key.cancel();
        }
    }

    private boolean readClient(SelectionKey key) throws IOException {
        ClientHolder.Client client = (ClientHolder.Client) key.attachment();
        SocketChannel clientSocket = (SocketChannel) key.channel();

        int readBytes = clientSocket.read(client.getReadBuffer());
        if (readBytes == -1)
            return false;

        client.readCountBytes += readBytes;
        if (client.getReadBuffer().capacity() <= client.readCountBytes)
            client.resize();

        if (NIOUtils.readyMessage(client)) {
            client.readCountBytes -= Integer.BYTES + client.currentBufferLength;
            byte[] messageByteArray = new byte[client.currentBufferLength];
            client.getReadBuffer().flip();
            client.getReadBuffer().get(messageByteArray);
            client.getReadBuffer().compact();

            var metricContext = collectorMetrics.start(MetricType.HANDLE_CLIENT_TIME);
            var arrayRequest = Messages.ArrayRequest.parseFrom(messageByteArray);
            logger.info("SERVER: Client Read Request");

            tasksThreadPool.execute(
                () -> handlerRequests.handle(
                    new ClientDataWithMetric(new ClientHolder(client, clientSocket), metricContext),
                    arrayRequest
                )
            );

            client.currentBufferLength = null;
        }

        return true;
    }

    @Override
    public void close() {
        tasksThreadPool.shutdownNow();
    }
}
