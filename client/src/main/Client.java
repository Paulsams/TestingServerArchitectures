import messages.Messages;
import services.ServiceLocator;
import services.loggers.LoggerService;
import services.metrics.CollectorMetricsService;
import services.metrics.MetricType;
import testing.parameters.Parameter;
import testing.parameters.ParameterType;
import utils.BlockingArchitectureUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

public class Client {
    private final int id;
    private final Random random;
    private final Map<ParameterType, Parameter> parameters;
    private final LoggerService logger = ServiceLocator.get(LoggerService.class);
    private final CollectorMetricsService collectorMetrics = ServiceLocator.get(CollectorMetricsService.class);

    Client(int id, int seed, Map<ParameterType, Parameter> parameters) {
        this.id = id;
        this.random = new Random(seed);
        this.parameters = parameters;
    }

    void start(InetSocketAddress inetAddress) throws IOException {
        try (var socket = SocketChannel.open(inetAddress)) {
            var inputStream = new DataInputStream(socket.socket().getInputStream());
            var outputStream = new DataOutputStream(socket.socket().getOutputStream());

            int n = parameters.get(ParameterType.N).getValue();
            int x = parameters.get(ParameterType.X).getValue();
            int delta = parameters.get(ParameterType.DELTA).getValue();

            var metricContext = collectorMetrics.start(MetricType.CLIENT_FULL_LOOP_TIME);
            for (int i = 0; i < x; i++) {
                logger.log("CLIENT " + id + ": Start " + i + " Loop");

                var arrayRequest = Messages.ArrayRequest
                    .newBuilder()
                    .addAllArray(() -> (IntStream
                        .range(0, n).map(l -> random.nextInt()).iterator()
                    ))
                    .build();

                outputStream.writeInt(arrayRequest.getSerializedSize());
                arrayRequest.writeTo(outputStream);

                var response = Messages.ArrayResponse.parseFrom(
                    BlockingArchitectureUtils.readByteArrayFromStream(inputStream)
                );

                if (!isSorted(response.getSortedArrayList()))
                    throw new RuntimeException("The array sent from the server is not sorted");

                logger.log("CLIENT " + id + ": Stop " + i + " Loop");

                Thread.sleep(delta);
            }
            metricContext.stop();

            logger.important("CLIENT " + id + ": end all loops");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isSorted(List<Integer> array) {
        for (int i = 0; i < array.size() - 1; i++) {
            if (array.get(i) > array.get(i + 1)) {
                return false;
            }
        }
        return true;
    }
}
