package server;

import lombok.SneakyThrows;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import utils.Constants;
import zmq.Publisher;
import zmq.Subscriber;

import java.util.List;

public class ServerToServerCommunication implements Runnable {

    private final List<String> messagesToServers;
    private final List<String> messagesFromServers;

    private final ZContext context = new ZContext();

    private final Publisher serverPublisher;
    private final Subscriber serverSubscriber;

    private final MutableBoolean initialized;
    private final MutableBoolean hasToken;
    private final int id;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public ServerToServerCommunication(String sub_host, String port, int id,
                                       List<String> messagesFromServers, List<String> messagesToServers) {
        this.messagesToServers = messagesToServers;
        this.messagesFromServers = messagesFromServers;
        this.serverSubscriber = new Subscriber(sub_host, context, messagesFromServers);
        this.serverPublisher = new Publisher(port, context, messagesToServers);
        this.id = id;
        this.initialized = new MutableBoolean();
        this.hasToken = new MutableBoolean();

    }

    public ServerToServerCommunication(String sub_host, String port, int id,
                                       MutableBoolean hasToken, MutableBoolean initialized,
                                       List<String> messagesToServers, List<String> messagesFromServers) {
        this.messagesToServers = messagesToServers;
        this.messagesFromServers = messagesFromServers;
        this.serverSubscriber = new Subscriber(sub_host, context, messagesFromServers);
        this.serverPublisher = new Publisher(port, context, messagesToServers);
        this.id = id;
        this.hasToken = hasToken;
        this.initialized = initialized;
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

                    switch (message) {
                        case Constants.INITIALIZE_MESSAGE:
                            this.initialized.setTrue();
                            logger.info("Received INIT message");
                            if (this.id != 0) {
                                try {
                                    addToMessagesToSend(message);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                logger.info("Whole circle initialized");
                            }
                            break;
                        case Constants.TOKEN:
                            logger.info("GOT TOKEN");
                            synchronized (this.hasToken) {
                                this.hasToken.setTrue();
                                this.hasToken.notify();
                            }
                            break;
                        default:
                            //Handling broadcast here
                            break;
                    }
                });
                messagesFromServers.clear();
                Thread.sleep(1);
                messagesFromServers.notify();
            }
        }
    }

    private void handleMessage(String message) throws InterruptedException {
        String[] splitMessage = message.split("\\|");
        if (!(splitMessage[1] == String.valueOf(this.id))) {
            if (splitMessage[0].equals(String.valueOf(this.id))) {

            } else if (splitMessage[0].equals("*")) {

            } else {
                addToMessagesToSend(message);
            }
        }
    }

    private void addToMessagesToSend(String message) throws InterruptedException {
        synchronized (messagesToServers) {
            Thread.sleep(100);
            messagesToServers.add(message);
            logger.info(id + " Moved message: " + message);
            messagesToServers.notify();
        }
    }
}
