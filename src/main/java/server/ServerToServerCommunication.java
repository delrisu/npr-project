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

    private final List<String> messagesToSendClient;
    private final List<String> messagesToSendServer;
    private final List<String> receivedMessagesClient;
    private final List<String> receivedMessagesServer;

    private final ZContext context = new ZContext();

    private final Publisher serverPublisher;
    private final Subscriber serverSubscriber;

    private final MutableBoolean initialized;
    private final MutableBoolean hasToken;
    private final int id;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public ServerToServerCommunication(List<String> messagesToSendClient, List<String> receivedMessagesClient, String sub_host, String port, int id,
                                       MutableBoolean hasToken, MutableBoolean initialized,
                                       List<String> messagesToSendToServers, List<String> receivedMessagesServer) {
        this.messagesToSendClient = messagesToSendClient;
        this.receivedMessagesClient = receivedMessagesClient;
        this.messagesToSendServer = messagesToSendToServers;
        this.receivedMessagesServer = receivedMessagesServer;
        this.serverSubscriber = new Subscriber(sub_host, context, receivedMessagesServer, true);
        this.serverPublisher = new Publisher(port, context, messagesToSendToServers, true);
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
            new Thread(new Initiator(this.initialized, this.messagesToSendServer)).start();
        }
        handleReceivedMessages();

    }

    private void handleReceivedMessages() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (receivedMessagesServer) {
                while (receivedMessagesServer.size() == 0) {
                    receivedMessagesServer.wait();
                }
                receivedMessagesServer.forEach(message -> {
                    try {
                        switch (message) {
                            case Constants.INITIALIZE_MESSAGE:
                                this.initialized.setTrue();
                                logger.info("Received INIT message");
                                if (this.id != 0) {
                                    addToList(message, this.messagesToSendServer);
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
                                logger.info("SENDING TOKEN TO SECOND PART");
                                addToList(Constants.TOKEN, this.receivedMessagesClient);
                                //System.out.println(this.receivedMessagesClient.size());
                                //addToList(Constants.TOKEN, this.messagesToSendServer);
                                logger.info("GOT TOKEN @");
                                break;
                            case Constants.UNLOCK:
                                synchronized (this.hasToken) {
                                    this.hasToken.setFalse();
                                    this.hasToken.notify();
                                }
                                addToList(Constants.TOKEN, this.messagesToSendServer);
                            case Constants.NOTIFY:
                                addToList(Constants.S_NOTIFY, this.receivedMessagesClient);
                                break;
                            default:
                                handleMessage(message);
                                break;
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                receivedMessagesServer.clear();
                Thread.sleep(1);
                receivedMessagesServer.notify();
            }
        }
    }

    private void handleMessage(String message) throws InterruptedException {
        String[] splitMessage = message.split("\\|");
        if (!(splitMessage[1] == String.valueOf(this.id))) {
            if (splitMessage[0].equals(String.valueOf(this.id))) {
                logger.info(message);
            } else if (splitMessage[0].equals("*")) {
                if (splitMessage[2].equals(Constants.NOTIFY_ALL)) {
                    addToList(Constants.S_NOTIFY_ALL, receivedMessagesClient);
                }
            } else {
                addToList(message, this.messagesToSendServer);
            }
        } else {
            logger.info(message);
        }
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
