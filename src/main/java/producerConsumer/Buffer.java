package producerConsumer;

import client.Monitor;
import lombok.Data;

@Data
public class Buffer {

    private final Monitor monitor;
    private Integer[] buffer = new Integer[10];

    public Buffer(String subscriberHost, String publisherHost) {
        this.monitor = new Monitor(subscriberHost, publisherHost, buffer);
    }

    public void put(int v) {

        buffer = (Integer[]) monitor._lock();

        while (getNextEmpty() == buffer.length) {
            buffer = (Integer[]) monitor._wait();
        }

        buffer[getNextEmpty()] = v;

        monitor._notifyAll();
        monitor._unlock();

    }

    public int get() {
        buffer = (Integer[]) monitor._lock();
        while (getNextEmpty() == 0){
            buffer = (Integer[]) monitor._wait();
        }

        int val = buffer[getNextEmpty() - 1];

        buffer[getNextEmpty() - 1] = null;

        monitor._notifyAll();
        monitor._unlock();
        return val;
    }

    private int getNextEmpty() {
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == null) {
                return i;
            }
        }
        return buffer.length;
    }
}
