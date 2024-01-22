package servers;

import messages.Messages;

import java.nio.ByteBuffer;

public class NIOUtils {
    @FunctionalInterface
    public interface OnSendResponse<TClient> {
        void onSend(TClient client, ByteBuffer writeBuffer);
    }

    public static boolean readyMessage(ClientOnServer client) {
        if (client.currentBufferLength == null && client.readCountBytes >= Integer.BYTES) {
            client.getReadBuffer().flip();
            client.currentBufferLength = client.getReadBuffer().getInt();
            client.getReadBuffer().compact();
        }

        return client.currentBufferLength != null &&
            client.readCountBytes >= Integer.BYTES + client.currentBufferLength;
    }

    public static<TClient> void sendResponse(
        TClient clientHolder,
        Messages.ArrayResponse response,
        OnSendResponse<TClient> callbackClientReadyFromWrite
    ) {
        var byteArray = response.toByteArray();

        ByteBuffer writeBuffer = createByteBufferFromByteArray(byteArray);
        writeBuffer.flip();

        callbackClientReadyFromWrite.onSend(clientHolder, writeBuffer);
    }

    private static ByteBuffer createByteBufferFromByteArray(byte[] byteArray) {
        var byteBuffer = ByteBuffer.allocate(Integer.BYTES + byteArray.length);
        byteBuffer.putInt(byteArray.length);
        byteBuffer.put(byteArray);
        return byteBuffer;
    }
}
