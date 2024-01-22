package servers;

import java.nio.ByteBuffer;

import static servers.ServerConstants.COEFFICIENT_INCREASE_READ_BUFFER;

public abstract class ClientOnServer {
    private ByteBuffer readBuffer;
    public Integer currentBufferLength = null;
    public int readCountBytes = 0;

    protected ClientOnServer(ByteBuffer readBuffer) {
        this.readBuffer = readBuffer;
    }

    public ByteBuffer getReadBuffer() { return readBuffer; }

    public void resize() {
        int oldPosition = readBuffer.position();
        int newLimit = readBuffer.array().length * COEFFICIENT_INCREASE_READ_BUFFER;

        var newReadBuffer = ByteBuffer.allocate(newLimit);
        newReadBuffer.put(readBuffer.array());
        newReadBuffer.order(readBuffer.order());
        newReadBuffer.position(0);
        newReadBuffer.position(oldPosition).limit(newLimit);

        readBuffer = newReadBuffer;
    }
}
