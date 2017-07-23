package nickel;

import nickel.net.Chunk;
import nickel.util.Constant;
import nickel.util.PrintUtil;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static nickel.util.Constant.MIN_TICK_INTERVAL;
import static nickel.util.Constant.SERVER_PORT;

/**
 * Created by Murray on 21/07/2017
 */
public class Server {

    private static final String SERVER_COMMAND_SHUTDOWN = "shutdown";

    private static final int SOCKET_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);
    private static final int QUEUE_SIZE = 10;

    static {
        System.setProperty("nickel", "server_" + Constant.TIMESTAMP);
    }

    private final PrintUtil printUtil;
    private final ServerSocket serverSocket;

    private final List<Socket> clientSockets;
    private final BlockingQueue<Serializable> toSendQueue;

    public Server(ServerSocket serverSocket, PrintUtil printUtil) {
        this.serverSocket = serverSocket;
        this.printUtil = printUtil;

        this.clientSockets = new ArrayList<>();
        this.toSendQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    }

    public static void main(String[] args) {
        PrintUtil printUtil = new PrintUtil();
        try {
            ServerSocket serverSOcket = new ServerSocket(SERVER_PORT);
            serverSOcket.setSoTimeout(SOCKET_TIMEOUT);
            Server server = new Server(serverSOcket, printUtil);
            server.start();
        } catch (Exception e) {
            printUtil.printAndLog(e);
        }
    }

    private void start() {
        AbstractTask manageConnectionsTask = getManageConnectionsTask();
        Thread t0 = new Thread(manageConnectionsTask);
        t0.start();

        AbstractTask sendDataTask = getSendDataTask();
        Thread t1 = new Thread(sendDataTask);
        t1.start();

        AbstractTask consoleInputTask = getConsoleInputTask(manageConnectionsTask, sendDataTask);
        consoleInputTask.run();
    }

    private final Object lockClientSocketList = new Object();
    private AbstractTask getManageConnectionsTask() {
        return new AbstractTask(printUtil) {
            @Override
            protected void runTask() throws Exception {
                printUtil.printAndLog(String.format("Server listening on local port %s", serverSocket.getLocalSocketAddress()));
                while (!isCancelled()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        printUtil.printAndLog(String.format("Client connected from %s", clientSocket.getInetAddress()));
                        clientSockets.add(clientSocket);
                        synchronized (clientSockets) {
                            clientSockets.notify(); // wake up the "send data" thread if it's sleeping
                        }
                    } catch (SocketTimeoutException x) {
                        // do nothing, this will naturally happen when the serverSocket timeout rolls over
                    }
                    removeClosedConnections();
                }
                printUtil.printAndLog("Connection listener thread exiting");
            }

            private void removeClosedConnections() {
                synchronized (lockClientSocketList) {
                    clientSockets.removeIf(Socket::isClosed);
                }
            }
        };
    }

    private AbstractTask getSendDataTask() {
        return new AbstractTask(printUtil) {
            @Override
            protected void runTask() throws Exception {
                while(!isCancelled()) {
                    synchronized (clientSockets) {
                        if (clientSockets.isEmpty()) {
                            clientSockets.wait(); // sleep if we have no connections
                        }
                    }
                    sendDataToClients();
                }
                printUtil.printAndLog("Data transmission thread exiting");
            }

            private void sendDataToClients() throws InterruptedException {
                Serializable toSend = toSendQueue.take(); // do this outside the client list lock, so we can accept new connections

                synchronized (lockClientSocketList) {
                    long intervalStart = System.currentTimeMillis();

                    List<Integer> badIndices = new ArrayList<>();
                    byte[][] binaryChunks = Chunk.toByteArrays(toSend);
                    for (int i = 0; i < clientSockets.size(); i++) {
                        try {
                            sendDataToClient(clientSockets.get(i), binaryChunks);
                        } catch (IOException e) {
                            printUtil.printAndLog(e);
                            badIndices.add(i);
                        }
                    }
                    for (Integer badIndex : badIndices) {
                        Socket badSocket = clientSockets.get(badIndex);
                        printUtil.printAndLog(String.format("Closing and removing erroneous socket %s:%s", badSocket.getInetAddress(), badSocket.getPort()));
                        try {
                            badSocket.close();
                        } catch (IOException e) {
                            printUtil.printAndLog(e);
                        }
                        clientSockets.remove((int) badIndex);
                    }

                    // make sure we're not iterating quicker than the minimum tick interval
                    long timeToWaste = MIN_TICK_INTERVAL - (System.currentTimeMillis() - intervalStart);
                    if (timeToWaste > 0) {
                        Thread.sleep(timeToWaste);
                    }
                }
            }

            private void sendDataToClient(Socket clientSocket, byte[][] chunks) throws IOException {
                for (byte[] chunk : chunks) {
                    clientSocket.getOutputStream().write(chunk);
                }
            }
        };
    }

    private AbstractTask getConsoleInputTask(AbstractTask... tasks) {
        return new AbstractTask(printUtil) {
            @Override
            protected void runTask() throws Exception {
                Scanner scanner = new Scanner(System.in);
                String input;
                outer: while (!isCancelled()) {
                    input = scanner.nextLine().trim().toLowerCase();
                    switch (input) {
                        case SERVER_COMMAND_SHUTDOWN:
                            printUtil.printAndLog("Stopping server");
                            for (AbstractTask task : tasks) {
                                task.setCancelled(true);
                            }
                            printUtil.printAndLog("Console input thread exiting");
                            break outer;
                        case "meme":
                            toSendQueue.add(World.testMessage);
                            break;
                        default:
                            toSendQueue.add("Message from server: " + input);
                    }
                }
            }
        };
    }
}
