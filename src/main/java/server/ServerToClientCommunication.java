package server;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import utils.Constants;
import zmq.Publisher;
import zmq.Subscriber;

import java.util.ArrayList;
import java.util.List;

public class ServerToClientCommunication implements Runnable {

    private final List<String> messagesToSendClient;
    private final List<String> messagesToSendServer;
    private final List<String> receivedMessagesClient;
    private final List<String> receivedMessagesServer;

    private final Publisher publisher;
    private final Subscriber subscriber;

    private final int id;

    private final ZContext context = new ZContext();

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean locked;
    private boolean waiting;

    private List<String> temp;

    private String type;

    public ServerToClientCommunication(List<String> messagesToSendClient, List<String> messagesToSendServer, List<String> receivedMessagesClient,
                                       List<String> receivedMessagesServer, int id, String clientSubscriberPort, String clientPublisherPort, String type) {
        this.messagesToSendClient = messagesToSendClient;
        this.messagesToSendServer = messagesToSendServer;
        this.receivedMessagesClient = receivedMessagesClient;
        this.receivedMessagesServer = receivedMessagesServer;
        this.id = id;
        this.type = type;
        publisher = new Publisher(clientPublisherPort, context, messagesToSendClient, true);
        subscriber = new Subscriber(clientSubscriberPort, context, receivedMessagesClient, false);


    }

    @SneakyThrows
    @Override
    public void run() {
        new Thread(subscriber).start();
        new Thread(publisher).start();
        handleMessages();
    }

    private void handleMessages() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (this.receivedMessagesClient) {
                while (this.receivedMessagesClient.size() == 0) {
                    this.receivedMessagesClient.wait();
                }
                temp = new ArrayList<>(receivedMessagesClient);
                this.receivedMessagesClient.clear();
                this.receivedMessagesClient.notify();
            }
            this.temp.forEach(message -> {
                logger.info(message);
                String[] splitMessage = message.split("\\|");
                try {
                    switch (splitMessage[0]) {
                        case Constants.NOTIFY: //NOTIFY from client to server
                            addToList(Constants.NOTIFY, this.messagesToSendServer);
                            break;
                        case Constants.S_NOTIFY: //NOTIFY from server to client
                            if (waiting) {
                                waiting = false;
                                locked = true;
                                addToList(Constants.NOTIFY, this.messagesToSendClient);
                            } else {
                                addToList(Constants.NOTIFY, this.messagesToSendServer);
                            }
                            break;
                        case Constants.NOTIFY_ALL:
                            addToList(Constants.NOTIFY_ALL+"|"+this.type+"|"+this.id, this.messagesToSendServer);
                            break;
                        case Constants.S_NOTIFY_ALL:
                            if (waiting) {
                                waiting = false;
                                locked = true;
                            }
                            break;
                        case Constants.LOCK:
                            this.locked = true;
                            break;
                        case Constants.UNLOCK:
                            this.locked = false;
                            addToList(Constants.UNLOCK + "|" + this.type + "|" + splitMessage[1], this.receivedMessagesServer);
                            break;
                        case Constants.WAIT:
                            this.waiting = true;
                            break;
                        case Constants.TOKEN:
                            if (locked) {
                                addToList(Constants.UNLOCK + "|" + splitMessage[2], messagesToSendClient);
                            } else {
                                addToList(Constants.UNLOCK + "|" + this.type + "|" + splitMessage[2], receivedMessagesServer);
                            }
                            break;
                        default:
                            break;

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
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
