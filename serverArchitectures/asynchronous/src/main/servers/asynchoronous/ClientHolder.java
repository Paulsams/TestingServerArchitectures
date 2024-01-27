package servers.asynchoronous;

import servers.ClientOnServer;
import servers.ServerArchitectureException;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

class ClientHolder extends ClientOnServer {
    public final AsynchronousSocketChannel socketChannel;
    private final AsynchronousServerArchitecture serverArchitecture;


    protected ClientHolder(
        AsynchronousSocketChannel socketChannel,
        ByteBuffer readBuffer,
        AsynchronousServerArchitecture serverArchitecture
    ) {
        super(readBuffer);
        this.socketChannel = socketChannel;
        this.serverArchitecture = serverArchitecture;
    }

    public void catchError(ServerArchitectureException error) {
        serverArchitecture.acceptError(error);
    }
}
