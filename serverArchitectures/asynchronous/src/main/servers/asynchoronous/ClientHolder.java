package servers.asynchoronous;

import servers.ClientOnServer;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

class ClientHolder extends ClientOnServer {
    public final AsynchronousSocketChannel socketChannel;

    protected ClientHolder(AsynchronousSocketChannel socketChannel, ByteBuffer readBuffer) {
        super(readBuffer);
        this.socketChannel = socketChannel;
    }
}
