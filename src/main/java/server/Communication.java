package server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Communication implements Runnable {

    final static public Object publisherMonitor = new Object();
    final static public Object subscriberMonitor = new Object();
    List<String> messagesToSend = new ArrayList<>();
    List<String> receivedMessages = new ArrayList<>();
    UUID uuid = UUID.randomUUID();

    @Override
    public void run() {

    }

    private void handleReceivedMessages() {

    }
}
