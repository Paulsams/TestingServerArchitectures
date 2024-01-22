package servers;

import testing.parameters.Parameter;
import testing.parameters.ParameterType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

public interface ServerArchitecture {
    @FunctionalInterface
    interface OnServerInitialized {
        void onInitialized();
    }

    void start(
        InetSocketAddress inetAddress,
        Map<ParameterType, Parameter> parameters,
        OnServerInitialized callbackInitialized
    ) throws IOException;

    void stop();
}
