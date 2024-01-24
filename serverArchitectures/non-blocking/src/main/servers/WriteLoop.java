package servers;

import services.ServiceLocator;
import services.loggers.LoggerService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class WriteLoop {
    private final Queue<ClientHolder> waitingWritingClients = new ConcurrentLinkedQueue<>();
    private final Selector writeSelector;

    private final LoggerService logger = ServiceLocator.get(LoggerService.class);

    public WriteLoop(Selector writeSelector) {
        this.writeSelector = writeSelector;
    }

    public void writeClients() throws IOException {
        while (true) {
            var countSelected = writeSelector.select();
            logger.log("Write select return " + countSelected);

            // Processing
            Iterator<SelectionKey> keysIterator = writeSelector.selectedKeys().iterator();
            while (keysIterator.hasNext()) {
                SelectionKey key = keysIterator.next();
                ClientHolder.Client client = (ClientHolder.Client) key.attachment();
                SocketChannel clientSocket = (SocketChannel) key.channel();

                var writeBufferWithMetric = client.writeBuffers.peek();

                writeBufferWithMetric.metricContext().tryStop();
                clientSocket.write(writeBufferWithMetric.buffer());
                if (!writeBufferWithMetric.buffer().hasRemaining())
                    client.writeBuffers.poll();

                if (client.writeBuffers.isEmpty())
                    key.cancel();

                keysIterator.remove();
            }

            // Registering new clients
            while (!waitingWritingClients.isEmpty()) {
                ClientHolder clientHolder = waitingWritingClients.poll();
                if (clientHolder == null)
                    break;

                var waitingClient = clientHolder.client();
                waitingClient.writeBuffers.addAll(waitingClient.waitingBuffers);

                SelectionKey key = clientHolder.socketChannel().register(writeSelector, SelectionKey.OP_WRITE);
                key.attach(waitingClient);

                logger.log(
                "Client: " +
                    clientHolder.socketChannel().socket().getRemoteSocketAddress() +
                    " Registered from Write"
                );
            }
        }
    }

    public void addClient(ClientHolder clientHolder) {
        waitingWritingClients.add(clientHolder);
        writeSelector.wakeup();
    }
}
