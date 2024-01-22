import servers.ServerConstants;
import servers.ServerArchitecture;
import testing.parameters.ParameterType;
import testing.parameters.TestingParameters;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;

public class MainLoop {
    private final Client[] clients;
    private final TestingParameters parameters;

    private boolean isServerOnInitialized;

    public MainLoop(TestingParameters parameters) {
        this.parameters = parameters;
        clients = new Client[parameters.all().get(ParameterType.M).getValue()];
        for (int i = 0; i < clients.length; i++)
            clients[i] = new Client(i, i * 1000, parameters.all());
    }

    public void startTesting(ServerArchitecture server) {
        isServerOnInitialized = false;

        var inetAddress = new InetSocketAddress(ServerConstants.SERVER_IP, ServerConstants.SERVER_PORT);

        Thread[] clientThreads = new Thread[clients.length];
        Thread serverThread = new Thread(() -> {
            try {
                server.start(inetAddress, parameters.all(), () -> {
                    synchronized (this) {
                        isServerOnInitialized = true;
                        this.notify();
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.start();

        try {
            synchronized (this) {
                while (!isServerOnInitialized)
                    this.wait();
            }

            startClients(inetAddress, clientThreads);
            for (Thread clientThread : clientThreads)
                clientThread.join();

            server.stop();
            serverThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void startClients(InetSocketAddress inetAddress, Thread[] clientThreads) {
        for (int i = 0; i < clientThreads.length; i++) {
            int finalI = i;

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            var clientThread = new Thread(() -> {
                while (true) {
                    try {
                        clients[finalI].start(inetAddress);
                        break;
                    } catch (ConnectException ignored) {

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            clientThreads[i] = clientThread;
            clientThread.start();
        }
    }
}
