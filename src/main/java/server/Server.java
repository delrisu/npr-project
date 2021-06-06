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
    private final MutableBoolean tokenInit = new MutableBoolean(false);

    private final List<String> messagesToSendServer = new ArrayList<>();
    private final List<String> receivedMessagesServer = new ArrayList<>();

    private final List<String> messagesToSendClient = new ArrayList<>();
    private final List<String> receivedMessagesClient = new ArrayList<>();



    private String serverSubscriberHost;
    private String serverPublisherPort;
    private String clientSubscriberPort;
    private String clientPublisherPort;
    int id;

    private String type;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public Server(String serverSubscriberHost, String serverPublisherPort, String clientSubscriberPort,
                  String clientPublisherPort, int id, String type, boolean init) {
        this.serverPublisherPort = serverPublisherPort;
        this.serverSubscriberHost = serverSubscriberHost;
        this.clientPublisherPort = clientPublisherPort;
        this.clientSubscriberPort = clientSubscriberPort;
        this.id = id;
        this.type = type;
        this.tokenInit.setValue(init);

        new Thread(new ServerToServerCommunication(this.receivedMessagesClient,
                this.serverSubscriberHost, this.serverPublisherPort, this.id,
                this.hasToken, this.initialized, this.messagesToSendServer, this.receivedMessagesServer,
                this.type, this.tokenInit)).start();
        new Thread(new ServerToClientCommunication(this.messagesToSendClient, this.messagesToSendServer,
                this.receivedMessagesClient,
                this.receivedMessagesServer, this.id, this.clientSubscriberPort, this.clientPublisherPort,
                this.type)).start();
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
