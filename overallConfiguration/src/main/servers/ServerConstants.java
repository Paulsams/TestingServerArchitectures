package servers;

public class ServerConstants {
    public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 8081;
    public static final int SIZE_THREAD_POOL_FOR_TASKS = 12;
    public static final int START_READ_BUFFER_SIZE_ON_SERVER_BY_CLIENT = 501 * Integer.BYTES;
    public static final int COEFFICIENT_INCREASE_READ_BUFFER = 2;
    public static final int BACKLOG = 200;
}
