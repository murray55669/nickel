package nickel.net;

import nickel.World;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static nickel.util.Constant.PACKET_CONTENT_MAX_BYTES;
import static nickel.util.Constant.PACKET_HEADER_BYTES;
import static nickel.util.Constant.PACKET_MAX_BYTES;

/**
 * Created by Murray on 21/07/2017
 *
 * NOTE: Java has no support for unsigned data types, hence the strange int-based workarounds
 *
 * TODO fix nomenclature
 */
public class Chunk {

    public static final int PACKET_LENGTH_INDEX = 0; // this should be the first thing read, as it allows us to then read the rest of the packet
    private static final int PACKET_ORDINAL_UPPER_BYTE_INDEX = 1;
    private static final int PACKET_ORDINAL_LOWER_BYTE_INDEX = 2;
    private static final int TOTAL_CHUNKS_UPPER_BYTE_INDEX = 3;
    private static final int TOTAL_CHUNKS_LOWER_BYTE_INDEX = 4;
    private static final int CHUNK_ORDINAL_UPPER_BYTE_INDEX = 5;
    private static final int CHUNK_ORDINAL_LOWER_BYTE_INDEX = 6;
    private static final int UNUSED_BYTE_INDEX = 7;

    private static final byte PLACEHOLDER_BYTE = 0x00;

    private static final AtomicInteger PACKET_ORDINAL = new AtomicInteger(0);
    private static final int MAX_UNSIGNED_SHORT = 65_535;

    public static Chunk[] toChunks(Serializable o) {
        ObjectAndClass toSend = new ObjectAndClass(o);
        byte[] asBytes = SerializationUtils.serialize(toSend);

        int packetNumber = getNextPacketOrdinal();
        int numberOfChunksRequired = (asBytes.length / PACKET_CONTENT_MAX_BYTES)+1;

        Chunk[] out = new Chunk[numberOfChunksRequired];
        byte[] chunk = new byte[PACKET_MAX_BYTES];

        int outIndex = 0;
        for (int i = 0; i < asBytes.length; i += PACKET_CONTENT_MAX_BYTES) {
            int numBytesRemaining = asBytes.length - i;
            if (numBytesRemaining < PACKET_CONTENT_MAX_BYTES) {
                fillHeader(packetNumber, numberOfChunksRequired, i, chunk, numBytesRemaining);
                System.arraycopy(asBytes, i, chunk, PACKET_HEADER_BYTES, numBytesRemaining);
            } else {
                fillHeader(packetNumber, numberOfChunksRequired, i, chunk, PACKET_CONTENT_MAX_BYTES);
                System.arraycopy(asBytes, i, chunk, PACKET_HEADER_BYTES, PACKET_CONTENT_MAX_BYTES);
            }
            out[outIndex] = new Chunk(chunk);
            outIndex++;
        }
        return out;
    }

    // TODO dedupe this; what is required?
    public static byte[][] toByteArrays(Serializable o) {
        ObjectAndClass toSend = new ObjectAndClass(o);
        byte[] asBytes = SerializationUtils.serialize(toSend);

        int packetNumber = getNextPacketOrdinal();
        int numberOfChunksRequired = (asBytes.length / PACKET_CONTENT_MAX_BYTES)+1;

        byte[][] out = new byte[numberOfChunksRequired][];

        int outIndex = 0;
        for (int i = 0; i < asBytes.length; i += PACKET_CONTENT_MAX_BYTES) {
            int numBytesRemaining = asBytes.length - i;
            byte[] chunk;
            if (numBytesRemaining < PACKET_CONTENT_MAX_BYTES) {
                chunk = new byte[numBytesRemaining+PACKET_HEADER_BYTES];
                fillHeader(packetNumber, numberOfChunksRequired, outIndex, chunk, numBytesRemaining);
                System.arraycopy(asBytes, i, chunk, PACKET_HEADER_BYTES, numBytesRemaining);
            } else {
                chunk = new byte[PACKET_MAX_BYTES];
                fillHeader(packetNumber, numberOfChunksRequired, outIndex, chunk, PACKET_CONTENT_MAX_BYTES);
                System.arraycopy(asBytes, i, chunk, PACKET_HEADER_BYTES, PACKET_CONTENT_MAX_BYTES);
            }
            out[outIndex] = chunk;
            outIndex++;
        }
        return out;
    }

    public static ObjectAndClass fromChunks(Chunk[] chunks) {
        int totalLength = Arrays.stream(chunks).map(e -> e.length).reduce(0, (a, b) -> a + b);
        byte[] fromPackets = new byte[totalLength];
        int index = 0;
        for (Chunk chunk : chunks) {
            System.arraycopy(chunk.contents, 0, fromPackets, index, chunk.length);
            index += chunk.length;
        }
        return SerializationUtils.deserialize(fromPackets);
    }

    private static int getNextPacketOrdinal() {
        int packetNumber;
        synchronized (PACKET_ORDINAL) {
            packetNumber = PACKET_ORDINAL.getAndIncrement();
            if (packetNumber >= MAX_UNSIGNED_SHORT) {
                PACKET_ORDINAL.set(0);
            }
        }
        return packetNumber;
    }

    private static void fillHeader(int packetOrdinal, int totalChunks, int chunkOrdinal, byte[] chunk, int packetContentLength) {
        // packet content length
        chunk[PACKET_LENGTH_INDEX] = (byte) packetContentLength;

        // packet ordinal number
        chunk[PACKET_ORDINAL_UPPER_BYTE_INDEX] = getUpperByte(packetOrdinal);
        chunk[PACKET_ORDINAL_LOWER_BYTE_INDEX] = getLowerByte(packetOrdinal);

        // chunks in this packet
        chunk[TOTAL_CHUNKS_UPPER_BYTE_INDEX] = getUpperByte(totalChunks);
        chunk[TOTAL_CHUNKS_LOWER_BYTE_INDEX] = getLowerByte(totalChunks);

        // chunk ordinal number
        chunk[CHUNK_ORDINAL_UPPER_BYTE_INDEX] = getUpperByte(chunkOrdinal);
        chunk[CHUNK_ORDINAL_LOWER_BYTE_INDEX] = getLowerByte(chunkOrdinal);

        // unused byte
        chunk[UNUSED_BYTE_INDEX] = PLACEHOLDER_BYTE;
    }
    private static byte getUpperByte(int unsignedShort) {
        return (byte) ((unsignedShort >> 8) & 0xFF);
    }
    private static byte getLowerByte(int unsignedShort) {
        return (byte) (unsignedShort & 0xFF);
    }
    private static int getUnsignedShort(byte upperByte, byte lowerByte) {
        return (((int) upperByte & 0xFF) << 8) + ((int) lowerByte & 0xFF);
    }
    private static int getUnsignedByte(byte signedByte) {
        return (int) signedByte & 0xFF;
    }

    public final int packetOrdinal;
    public final int totalChunks;
    public final int chunkOrdinal;
    public final int length;
    public final byte[] contents;
    public Chunk(byte[] chunk) {
        length = getUnsignedByte(chunk[PACKET_LENGTH_INDEX]);
        packetOrdinal = getUnsignedShort(chunk[PACKET_ORDINAL_UPPER_BYTE_INDEX], chunk[PACKET_ORDINAL_LOWER_BYTE_INDEX]);
        totalChunks = getUnsignedShort(chunk[TOTAL_CHUNKS_UPPER_BYTE_INDEX], chunk[TOTAL_CHUNKS_LOWER_BYTE_INDEX]);
        chunkOrdinal = getUnsignedShort(chunk[CHUNK_ORDINAL_UPPER_BYTE_INDEX], chunk[CHUNK_ORDINAL_LOWER_BYTE_INDEX]);
        contents = new byte[length];
        System.arraycopy(chunk, PACKET_HEADER_BYTES, contents, 0, length);
    }

    public static class ObjectAndClass implements Serializable {
        public Serializable o;
        public Class c;
        ObjectAndClass(Serializable o) {
            this.o = o;
            this.c = o.getClass();
        }
    }

    public static void main(String[] args) { // FIXME test code; remove

        Chunk[] chunks = Chunk.toChunks(World.testMessage);
        System.out.println(String.format("Data was split into %d chunks. Recreated data:\n%s", chunks.length, Chunk.fromChunks(chunks).o));

        byte[][] bytees = Chunk.toByteArrays(World.testMessage);
        System.out.println();
    }
}
