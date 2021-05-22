package server;

import lombok.SneakyThrows;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import utils.Constants;
import zmq.Publisher;
import zmq.Subscriber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerCommunication implements Runnable {

    private final List<String> messagesToServers = new ArrayList<>();
    private final List<String> messagesFromServers = new ArrayList<>();
    private final List<String> messagesFromClients = new ArrayList<>();
    private final List<String> messagesToClients = new ArrayList<>();

    private final ZContext context = new ZContext();

    private final Publisher serverPublisher;
    private final Publisher clientPublisher;
    private final Subscriber serverSubscriber;

    private final MutableBoolean initialized = new MutableBoolean(false);

    String serverPublisherPort;
    String clientPublisherPort;
    String subscriberHost;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean hasToken = false;
    private boolean notified = false;
    private int id;

    public ServerCommunication() throws IOException {
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
                    this.hasToken = true;
                    this.subscriberHost = hostsSplit.get(hostsSplit.size() - 1)[0];
                } else {
                    this.subscriberHost = hostsSplit.get(i - 1)[0];
                }
                this.clientPublisherPort = hostsSplit.get(i)[2];
            }
        }
        this.serverSubscriber = new Subscriber(subscriberHost, context, messagesFromServers);
        this.serverPublisher = new Publisher(serverPublisherPort, context, messagesToServers);
        this.clientPublisher = new Publisher(clientPublisherPort, context, messagesToClients);
    }

    public ServerCommunication(String sub_host, String port, String portC, int id) {
        this.serverSubscriber = new Subscriber(sub_host, context, messagesFromServers);
        this.serverPublisher = new Publisher(port, context, messagesToServers);
        this.clientPublisher = new Publisher(portC, context, messagesToClients);
        this.id = id;
    }

    @SneakyThrows
    @Override
    public void run() {
        logger.info("Running!");
        new Thread(serverSubscriber).start();
        new Thread(serverPublisher).start();


        if (this.id == 0) {
            new Thread(new Initiator(this.initialized, this.messagesToServers)).start();
        }
        handleReceivedMessages();

    }

    private void handleReceivedMessages() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (messagesFromServers) {
                while (messagesFromServers.size() == 0) {
                    messagesFromServers.wait();
                }
                messagesFromServers.forEach(message -> {
                    if (message.equals(Constants.INITIALIZE_MESSAGE)) {
                        this.initialized.setTrue();
                        logger.info("Received INIT message");
                        if (this.id != 0) {
                            addToMessagesToSend(message);
                        } else {
                            logger.info("Whole circle initialized");
                        }
                    } else {
                        addToMessagesToSend(message);
                    }
                });
                messagesFromServers.clear();
                Thread.sleep(1);
                messagesFromServers.notify();
            }
            synchronized (messagesFromClients) {

            }
        }
    }

    private void addToMessagesToSend(String message) {
        synchronized (messagesToServers) {
            messagesToServers.add(message);
            logger.info(id + " Moved message: " + message);
            messagesToServers.notify();
        }
    }
}
