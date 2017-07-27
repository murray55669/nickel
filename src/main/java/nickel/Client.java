package nickel;

import nickel.net.Chunk;
import nickel.util.Constant;
import nickel.util.PrintUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

import static nickel.net.Chunk.PACKET_LENGTH_INDEX;
import static nickel.util.Constant.*;

/**
 * Created by Murray on 21/07/2017
 */
public class Client {

    static {
        System.setProperty("nickel", "client_" + Constant.TIMESTAMP);
    }

    private final PrintUtil printUtil;
    private final Socket clientSocket;

    public Client(Socket clientSocket, PrintUtil printUtil) {
        this.clientSocket = clientSocket;
        this.printUtil = printUtil;
    }

    public void start() throws IOException {
        InputStream receiveStream = clientSocket.getInputStream();
        OutputStream sendStream = clientSocket.getOutputStream();


        AbstractTask sendTask = new AbstractTask(printUtil) {
            @Override
            protected void runTask() throws Exception {
                // TODO
            }
        };

        AbstractTask receiveTask = getReceiveTask(receiveStream);
        Thread receiveThread = new Thread(receiveTask);
        receiveThread.start();
    }

    private AbstractTask getReceiveTask(InputStream receiveStream) {
        return new AbstractTask(printUtil) {
            private Chunk[] collectedChunks;
            private int collectedChunkCount = -1;
            private int currentPacketOrdinal = -1;

            private byte[] receivedData = new byte[PACKET_MAX_BYTES];

            @Override
            protected void runTask() throws Exception {
                while(!isCancelled()) {
                    int chunkContentLength = receiveStream.read();
                    if (chunkContentLength == -1) {
                        break;
                    } else {
                        receivedData[PACKET_LENGTH_INDEX] = (byte) chunkContentLength;
                        receiveStream.read(receivedData, 1, chunkContentLength+PACKET_HEADER_BYTES-1);
                        Chunk chunk = new Chunk(receivedData);

                        // if new chunk series, throw away any data previously collected
                        if (chunk.packetOrdinal > currentPacketOrdinal) {
                            collectedChunkCount = 0;
                            currentPacketOrdinal = chunk.packetOrdinal;
                            collectedChunks = new Chunk[chunk.totalChunks];
                        }
                        collectedChunks[chunk.chunkOrdinal] = chunk;
                        collectedChunkCount++;

                        // finalise the data, and do something with it
                        if (collectedChunkCount == chunk.totalChunks) {
                            Chunk.ObjectAndClass out = Chunk.fromChunks(collectedChunks);
                            System.out.println(out.o); // TODO
                        }
                    }
                }
            }
        };
    }

    public static void main(String[] args) {
        PrintUtil printUtil = new PrintUtil();

        try {
            Scanner scanner = new Scanner(System.in);
            printUtil.printAndLog("Enter a host address to connect (blank for localhost):");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.isEmpty()) {
                input = "localhost";
            }
            Socket clientSocket = new Socket(input, SERVER_PORT); // TODO allow specification of port
            printUtil.printAndLog(String.format("Connected to server at %s", clientSocket.getInetAddress()));
            Client client = new Client(clientSocket, printUtil);
            client.start();
        } catch (Exception e) {
            printUtil.printAndLog(e);
        }
    }
}
