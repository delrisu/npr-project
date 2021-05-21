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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class ServerCommunication implements Runnable {

    final static public Object publisherMonitor = new Object();
    final static public Object subscriberMonitor = new Object();
    private List<String> messagesToSend = new ArrayList<>();
    private List<String> receivedMessages = new ArrayList<>();
    private ZContext context = new ZContext();

    private boolean hasToken = false;

    private int id;
    private Publisher publisher;
    private Subscriber subscriber;

    private int[] waiting;

    String publisherPort;
    String subscriberHost;

    Logger logger = LoggerFactory.getLogger(this.getClass());

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

        waiting = new int[hostsSplit.size()];
        for (int i = 0; i < hostsSplit.size(); i++) {
            if(hostsSplit.get(i)[1].equals("1")){
                publisherPort = hostsSplit.get(i)[0].split(":")[1];
                id = i;
                if(i==0){
                    hasToken = true;
                    subscriberHost = hostsSplit.get(hostsSplit.size() - 1)[0];
                } else{
                    subscriberHost = hostsSplit.get(i - 1)[0];
                }
            }
        }
        subscriber = new Subscriber(subscriberHost, "A", context, receivedMessages);
        publisher = new Publisher(publisherPort, context, messagesToSend);

    }

    @SneakyThrows
    @Override
    public void run() {
        logger.info("Running!");
        new Thread(subscriber).start();
        new Thread(publisher).start();
//        if(id == 1) {
///*
////////////////////////////////////////////////////////////////////////
//                        INICJALIZACJA
////////////////////////////////////////////////////////////////////////
// */
//            Executors.newSingleThreadExecutor().execute(new Runnable() {
//                @Override
//                public void run() {
//
//                }
//            });
//        }
        handleReceivedMessages();
    }

    private void handleReceivedMessages() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (ServerCommunication.subscriberMonitor) {
                while (receivedMessages.size() == 0) {
//                    logger.info("Waiting");
                    ServerCommunication.subscriberMonitor.wait();
                }
                receivedMessages.forEach(message -> {
                    synchronized (ServerCommunication.publisherMonitor) {
                        messagesToSend.add(message);
                        logger.info("Moved message: " + message);
                        ServerCommunication.publisherMonitor.notify();
                    }
                });
                receivedMessages.clear();
                Thread.sleep(1);
                ServerCommunication.subscriberMonitor.notify();
            }
        }
    }
}
