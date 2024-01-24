package servers.asynchoronous;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

class WriteCompletionHandler implements CompletionHandler<Integer, ClientDataWithMetric> {
    private final ByteBuffer writeBuffer;

    public WriteCompletionHandler(ByteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    @Override
    public void completed(Integer result, ClientDataWithMetric clientDataWithMetric) {
        clientDataWithMetric.metricContext().tryStop();

        var client = clientDataWithMetric.clientHolder();
        if (writeBuffer.hasRemaining())
            client.socketChannel.write(writeBuffer, clientDataWithMetric, this);
    }

    @Override
    public void failed(Throwable exc, ClientDataWithMetric attachment) {
        throw new RuntimeException(exc);
    }
}
