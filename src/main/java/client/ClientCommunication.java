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

    private final ZContext context = new ZContext();

    private Publisher publisher;
    private Subscriber subscriber;

    private List<String> temp;

    private final Object waitMonitor;
    private final Object lockMonitor;

    private final Monitor monitor;

    public ClientCommunication(List<String> received, List<String> toSend, String subscriberHost, String publisherHost, Object waitMonitor, Object lockMonitor, Monitor monitor) {
        this.received = received;
        this.toSend = toSend;
        this.waitMonitor = waitMonitor;
        this.lockMonitor = lockMonitor;
        this.monitor = monitor;
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

            switch (splitMessage[0]){
                case Constants.NOTIFY:
                    synchronized (waitMonitor){
                        waitMonitor.notify();
                    }
                    break;
                case Constants.UNLOCK:
                    System.out.println(splitMessage[1]);
                    monitor.setObject(new Gson().fromJson(splitMessage[1], monitor.getObject().getClass()));
                    synchronized (lockMonitor){
                        lockMonitor.notify();
                    }
                    break;
            }
        });
    }
}
