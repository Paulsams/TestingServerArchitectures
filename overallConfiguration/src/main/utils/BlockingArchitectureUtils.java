package utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BlockingArchitectureUtils {
    public static byte[] readByteArrayFromStream(DataInputStream inputStream) throws IOException {
        var lengthBuffer = inputStream.readInt();

        return readByteArray(inputStream, lengthBuffer);
    }

    private static byte[] readByteArray(InputStream inputStream, int lengthBuffer) throws IOException {
        var byteBuffer = new byte[lengthBuffer];
        var readLengthBuffer = 0;
        while (lengthBuffer != readLengthBuffer) {
            int countReadBytes = inputStream.read(
                byteBuffer, readLengthBuffer, lengthBuffer - readLengthBuffer
            );
            if (countReadBytes >= 0)
                readLengthBuffer += countReadBytes;
        }
        return byteBuffer;
    }
}
