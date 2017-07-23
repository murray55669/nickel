package nickel.util;

/**
 * Created by Murray on 21/07/2017
 */
public class Constant {

    // TODO split this into multiple files probably

    // NETWORKING
    public static final int SERVER_PORT = 12345;
    public static final int PACKET_MAX_BYTES = 248;
    public static final int PACKET_HEADER_BYTES = 8;
    public static final int PACKET_CONTENT_MAX_BYTES = PACKET_MAX_BYTES - PACKET_HEADER_BYTES;

    // DATE
    public static final String TIMESTAMP = DateUtil.getTimestamp();

    public static int MIN_TICK_INTERVAL = 10;
}
