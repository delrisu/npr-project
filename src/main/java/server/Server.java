package server;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {

    private final MutableBoolean hasToken = new MutableBoolean(false);
    private final MutableBoolean initialized = new MutableBoolean(false);

    private final List<String> messagesToServers = new ArrayList<>();
    private final List<String> messagesFromServers = new ArrayList<>();

    private final List<String> messagesFromClients = new ArrayList<>();
    private final List<String> messagesToClients = new ArrayList<>();
    private final List<String> waitingClients = new ArrayList<>();

    String subscriberHost;
    String serverPublisherPort;
    String clientPusherPort; //?
    String clientPublisherPort; //?
    int id;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public Server() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Hosts");
        assert inputStream != null;
        List<String> lines = new ArrayList<>();
        List<String[]> hostsSplit = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        while (reader.ready()) {
            lines.add(reader.readLine());
        }
        logger.info("Hosts size: " + lines.size());
        Collections.sort(lines);
        lines.forEach(host -> hostsSplit.add(host.split("\\|")));

        for (int i = 0; i < hostsSplit.size(); i++) {
            if (hostsSplit.get(i)[1].equals("1")) {
                this.serverPublisherPort = hostsSplit.get(i)[0].split(":")[1];
                id = i;
                if (i == 0) {
                    synchronized (this.hasToken) {
                        this.hasToken.setTrue();
                        this.hasToken.notify();
                    }
                    this.subscriberHost = hostsSplit.get(hostsSplit.size() - 1)[0];
                } else {
                    this.subscriberHost = hostsSplit.get(i - 1)[0];
                }
                this.clientPusherPort = hostsSplit.get(i)[2];
            }
        }

        new Thread(new ServerToServerCommunication(this.subscriberHost, this.serverPublisherPort, this.id,
                this.hasToken, this.initialized, messagesToServers, messagesFromServers)).start();
    }

    public Server(String subscriberHost, String serverPublisherPort, String clientPusherPort, int id) {
        this.serverPublisherPort = serverPublisherPort;
        this.clientPusherPort = clientPusherPort;
        this.subscriberHost = subscriberHost;
        this.id = id;

        new Thread(new ServerToServerCommunication(this.subscriberHost, this.serverPublisherPort, this.id,
                this.hasToken, this.initialized, messagesToServers, messagesFromServers)).start();
    }
}
