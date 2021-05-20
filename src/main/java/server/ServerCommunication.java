package server;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ServerCommunication implements Runnable {

    final static public Object publisherMonitor = new Object();
    final static public Object subscriberMonitor = new Object();
    List<String> messagesToSend = new ArrayList<>();
    List<String> receivedMessages = new ArrayList<>();
    ZContext context = new ZContext();

    String id;
    Publisher publisher;
    Subscriber subscriber;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public ServerCommunication() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Hosts");
        assert inputStream != null;
        List<String[]> hosts = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        while (reader.ready()) {
            hosts.add(reader.readLine().split("\\|"));
        }
        logger.info("Hosts size: " + hosts.size());
        hosts.forEach(host -> System.out.println(host[0] + " " + host[1]));

    }

    @SneakyThrows
    @Override
    public void run() {
        logger.info("Running!");
        Subscriber subscriber = new Subscriber("127.0.0.1:55555", "A", context, receivedMessages);
        new Thread(subscriber).start();
        handleReceivedMessages();
    }

    private void handleReceivedMessages() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (ServerCommunication.subscriberMonitor) {
                while (receivedMessages.size() == 0) {
                    logger.info("WAITING");
                    ServerCommunication.subscriberMonitor.wait();
                }
                receivedMessages.forEach(System.out::println);
                receivedMessages.clear();
                ServerCommunication.subscriberMonitor.notify();
            }
        }
    }
}
