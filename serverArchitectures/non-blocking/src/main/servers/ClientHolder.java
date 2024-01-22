package servers;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

record ClientHolder(Client client, SocketChannel socketChannel) {
    static class Client extends ClientOnServer {
        public final Queue<ByteBuffer> writeBuffers = new LinkedList<>();
        public final Queue<ByteBuffer> waitingBuffers = new ConcurrentLinkedDeque<>();

        protected Client(ByteBuffer readBuffer) {
            super(readBuffer);
        }
    }
}
