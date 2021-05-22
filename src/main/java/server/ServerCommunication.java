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

    private final List<String> messagesToSend = new ArrayList<>();
    private final List<String> receivedMessages = new ArrayList<>();
    private final ZContext context = new ZContext();
    private final Publisher publisher;
    private final Subscriber subscriber;
    String publisherPort;
    String subscriberHost;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private boolean hasToken = false;
    private boolean notified = false;
    private MutableBoolean initialized = new MutableBoolean(false);
    private int id;

    public ServerCommunication() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Hosts");
        assert inputStream != null;
        List<String> hosts = new ArrayList<>();
        List<String[]> hostsSplit = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        while (reader.ready()) {
            hosts.add(reader.readLine());
        }
        logger.info("Hosts size: " + hosts.size());
        Collections.sort(hosts);
        hosts.forEach(host -> hostsSplit.add(host.split("\\|")));

        for (int i = 0; i < hostsSplit.size(); i++) {
            if (hostsSplit.get(i)[1].equals("1")) {
                publisherPort = hostsSplit.get(i)[0].split(":")[1];
                id = i;
                if (i == 0) {
                    hasToken = true;
                    subscriberHost = hostsSplit.get(hostsSplit.size() - 1)[0];
                } else {
                    subscriberHost = hostsSplit.get(i - 1)[0];
                }
            }
        }
        subscriber = new Subscriber(subscriberHost, "A", context, receivedMessages);
        publisher = new Publisher(publisherPort, context, messagesToSend);
    }

    public ServerCommunication(String sub_host, String port, int id) {
        subscriber = new Subscriber(sub_host, "A", context, receivedMessages);
        publisher = new Publisher(port, context, messagesToSend);
        this.id = id;
    }

    @SneakyThrows
    @Override
    public void run() {
        logger.info("Running!");
        new Thread(subscriber).start();
        new Thread(publisher).start();


        if (this.id == 0) {
            new Thread(new Initiator(this.initialized, this.messagesToSend)).start();
        }
        while (!Thread.currentThread().isInterrupted()) {
            handleReceivedMessages();
        }
    }

    private void handleReceivedMessages() throws InterruptedException {
        synchronized (receivedMessages) {
            while (receivedMessages.size() == 0) {
                receivedMessages.wait();
            }
            receivedMessages.forEach(message -> {
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
            receivedMessages.clear();
            Thread.sleep(1);
            receivedMessages.notify();
        }
    }

    private void addToMessagesToSend(String message) {
        synchronized (messagesToSend) {
            messagesToSend.add(message);
            logger.info(id + " Moved message: " + message);
            messagesToSend.notify();
        }
    }
}
