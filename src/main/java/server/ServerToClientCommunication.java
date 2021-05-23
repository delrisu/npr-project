package server;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.zeromq.ZContext;

import java.util.List;

public class ServerToClientCommunication implements Runnable {

    private final List<String> messagesFromClients;
    private final List<String> messagesToClients;
    private final List<String> messagesToServers;

    private final MutableBoolean initialized;

    private final int id;

    private final ZContext context = new ZContext();

    public ServerToClientCommunication(List<String> messagesFromClients, List<String> messagesToClients,
                                       MutableBoolean hasToken, List<String> messagesToServers, MutableBoolean initialized, int id) {
        this.messagesFromClients = messagesFromClients;
        this.messagesToClients = messagesToClients;
        this.messagesToServers = messagesToServers;
        this.initialized = initialized;
        this.id = id;
    }

    @Override
    public void run() {
    }
}
