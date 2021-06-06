package client;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.zeromq.ZContext;
import utils.Constants;
import zmq.Publisher;
import zmq.Subscriber;

import java.util.ArrayList;
import java.util.List;

public class ClientCommunication implements Runnable {

    private final List<String> received;
    private final List<String> toSend;

    private final Publisher publisher;
    private final Subscriber subscriber;

    private final Object waitMonitor;
    private final Object lockMonitor;

    private final Monitor monitor;

    public ClientCommunication(List<String> received, List<String> toSend, String subscriberHost, String publisherHost, Object waitMonitor, Object lockMonitor, Monitor monitor) {
        this.received = received;
        this.toSend = toSend;
        this.waitMonitor = waitMonitor;
        this.lockMonitor = lockMonitor;
        this.monitor = monitor;
        ZContext context = new ZContext();
        subscriber = new Subscriber(subscriberHost, context, received, true);
        publisher = new Publisher(publisherHost, context, toSend, false);

    }


    @SneakyThrows
    @Override
    public void run() {
        new Thread(subscriber).start();
        new Thread(publisher).start();
        handleReceivedMessages();
    }

    private void handleReceivedMessages() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            List<String> temp;
            synchronized (received) {
                while (received.size() == 0) {
                    received.wait();
                }
                temp = new ArrayList<>(received);
                received.clear();
                received.notify();
            }

            temp.forEach(message -> {
                String[] splitMessage = message.split("\\|");

                switch (splitMessage[0]) {
                    case Constants.NOTIFY:
//                        System.out.println("BABY I'M CLIENT AND I'VE BEEN NOTIFIED");
                        synchronized (waitMonitor) {
//                            System.out.println("NOTIFY");
                            waitMonitor.notify();
                        }
//                        System.out.println("NOTIFIED");
                        break;
                    case Constants.UNLOCK:
                        if (!splitMessage[1].equals("X")) {
                            monitor.setObject(new Gson().fromJson(splitMessage[1], monitor.getObject().getClass()));
                        }
                        synchronized (lockMonitor) {
                            lockMonitor.notify();
                        }
                        break;
                }
            });
        }
    }
}
