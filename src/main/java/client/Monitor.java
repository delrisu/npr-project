package client;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class Monitor {

    private final List<String> received = new ArrayList<>();
    private final List<String> toSend = new ArrayList<>();
    private final Object waitMonitor = new Object();
    private final Object lockMonitor = new Object();
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Setter
    @Getter
    private Object object;


    public Monitor(String subscriberHost, String publisherHost, Object object) {
        new Thread(new ClientCommunication(received, toSend, subscriberHost, publisherHost, waitMonitor, lockMonitor, this)).start();
        this.object = object;
    }

    public void _notify() {
        try {
            addToList(Constants.NOTIFY, this.toSend);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void _notifyAll() {
        try {
            addToList(Constants.NOTIFY_ALL, this.toSend);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void _wait() {
        try {
            addToList(Constants.WAIT, this.toSend);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (waitMonitor){
            try {
                waitMonitor.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Object _lock() {
        try {
            addToList(Constants.LOCK, this.toSend);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (lockMonitor){
            try {
                lockMonitor.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return this.object;
    }

    public Object _unlock() {
        try {
            addToList(Constants.UNLOCK + "|" + new Gson().toJson(object), this.toSend);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this.object;
    }

    private void addToList(String message, List<String> list) throws InterruptedException {
        synchronized (list) {
            Thread.sleep(100);
            list.add(message);
            logger.info("CLIENT Moved message: " + message);
            list.notify();
        }
    }
}
