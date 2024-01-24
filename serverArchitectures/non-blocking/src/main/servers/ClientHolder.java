package servers;

import services.metrics.CollectMetricContext;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

record ClientHolder(Client client, SocketChannel socketChannel) {
    public record WriteBufferWithMetricContext(ByteBuffer buffer, CollectMetricContext metricContext) { }

    static class Client extends ClientOnServer {
        public final Queue<WriteBufferWithMetricContext> writeBuffers = new LinkedList<>();
        public final Queue<WriteBufferWithMetricContext> waitingBuffers = new ConcurrentLinkedDeque<>();

        protected Client(ByteBuffer readBuffer) {
            super(readBuffer);
        }
    }
}
