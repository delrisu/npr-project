package server;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Constants;

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

    private final List<String> messagesToSendServer = new ArrayList<>();
    private final List<String> receivedMessagesServer = new ArrayList<>();

    private final List<String> messagesToSendClient = new ArrayList<>();
    private final List<String> receivedMessagesClient = new ArrayList<>();
    private final List<String> waitingClients = new ArrayList<>();

    String serverSubscriberHost;
    String serverPublisherPort;
    String clientSubscriberPort;
    String clientPublisherPort;
    int id;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public Server() throws IOException, InterruptedException {
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
                    this.serverSubscriberHost = hostsSplit.get(hostsSplit.size() - 1)[0];
                    addToList(Constants.TOKEN, this.receivedMessagesServer);
                } else {
                    this.serverSubscriberHost = hostsSplit.get(i - 1)[0];
                }
                //this.clientPusherPort = hostsSplit.get(i)[2];
            }
        }

        new Thread(new ServerToServerCommunication(messagesToSendClient, receivedMessagesClient,
                this.serverSubscriberHost, this.serverPublisherPort, this.id,
                this.hasToken, this.initialized, messagesToSendServer, receivedMessagesServer)).start();
    }

    public Server(String serverSubscriberHost, String serverPublisherPort, String clientSubscriberPort,
                  String clientPublisherPort, int id) {
        this.serverPublisherPort = serverPublisherPort;
        this.serverSubscriberHost = serverSubscriberHost;
        this.clientPublisherPort = clientPublisherPort;
        this.clientSubscriberPort = clientSubscriberPort;
        this.id = id;

        new Thread(new ServerToServerCommunication(this.messagesToSendClient, this.receivedMessagesClient,
                this.serverSubscriberHost, this.serverPublisherPort, this.id,
                this.hasToken, this.initialized, this.messagesToSendServer, this.receivedMessagesServer)).start();
        new Thread(new ServerToClientCommunication(this.messagesToSendClient, this.messagesToSendServer, this.receivedMessagesClient,
                this.receivedMessagesServer, this.id, this.clientSubscriberPort, this.clientPublisherPort)).start();
    }

    private void addToList(String message, List<String> list) throws InterruptedException {
        synchronized (list) {
            Thread.sleep(100);
            list.add(message);
            logger.info(id + " Moved message: " + message);
            list.notify();
        }
    }
}
