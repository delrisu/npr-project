package server;

import lombok.SneakyThrows;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import utils.Constants;
import zmq.Publisher;
import zmq.Subscriber;

import java.util.ArrayList;
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
    private final MutableBoolean tokenInit;
    private final int id;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private List<String> temp;
    private String type;

    public ServerToServerCommunication(List<String> messagesToSendClient, List<String> receivedMessagesClient, String sub_host, String port, int id,
                                       MutableBoolean hasToken, MutableBoolean initialized,
                                       List<String> messagesToSendToServers, List<String> receivedMessagesServer, String type, MutableBoolean tokenInit) {
        this.messagesToSendClient = messagesToSendClient;
        this.receivedMessagesClient = receivedMessagesClient;
        this.messagesToSendServer = messagesToSendToServers;
        this.receivedMessagesServer = receivedMessagesServer;
        this.serverSubscriber = new Subscriber(sub_host, context, receivedMessagesServer, true);
        this.serverPublisher = new Publisher(port, context, messagesToSendToServers, true);
        this.id = id;
        this.hasToken = hasToken;
        this.initialized = initialized;
        this.type = type;
        this.tokenInit = tokenInit;
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
                temp = new ArrayList<>(receivedMessagesServer);
                receivedMessagesServer.clear();
                receivedMessagesServer.notify();
            }
            temp.forEach(message -> {
                logger.info(this.id + " " + this.type + " " + message);
                String[] splitMessage = message.split("\\|");
                try {
                    switch (splitMessage[0]) {
                        case Constants.INITIALIZE_MESSAGE:
                            this.initialized.setTrue();
                            logger.info("Received INIT message");
                            if (this.id != 0) {
                                addToList(message, this.messagesToSendServer);
                            } else {
                                logger.info("Whole circle initialized");
                                addToList(Constants.WHOLE_CIRCLE, this.messagesToSendServer);
                            }
                            break;
                        case Constants.WHOLE_CIRCLE:
                            if (this.id != 0) {
                                addToList(message, this.messagesToSendServer);
                            }
                            if (this.tokenInit.getValue()) {
                                addToList(Constants.TOKEN + "|" + this.type + "|X", this.receivedMessagesServer);
                            }
                            break;
                        default:
                            handleMessage(message);
                            break;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

        }
    }

    private void handleMessage(String message) {
        String[] splitMessage = message.split("\\|");
        try {
            if (splitMessage[1].equals(this.type)) {
                switch (splitMessage[0]) {
                    case Constants.TOKEN:
                        logger.info(this.id + " Received correct token!");
                        synchronized (this.hasToken) {
                            this.hasToken.setTrue();
                            this.hasToken.notify();
                        }
                        addToList(Constants.TOKEN + "|" + this.type + "|"+ splitMessage[2], this.receivedMessagesClient);
                        break;
                    case Constants.UNLOCK:
                        synchronized (this.hasToken) {
                            this.hasToken.setFalse();
                            this.hasToken.notify();
                        }
                        addToList(Constants.TOKEN + "|" + this.type + "|" + splitMessage[2], this.messagesToSendServer);
                        break;
                    case Constants.NOTIFY:
                        addToList(Constants.S_NOTIFY, this.receivedMessagesClient);
                        break;
                    case Constants.NOTIFY_ALL:
                        if(!splitMessage[2].equals(String.valueOf(this.id))) {
                            addToList(Constants.S_NOTIFY_ALL, this.receivedMessagesClient);
                            addToList(message, this.messagesToSendServer);
                        }
                        break;
                }
            } else {
                addToList(message, this.messagesToSendServer);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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
