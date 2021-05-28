package producerConsumer;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer implements Runnable{

    private final Buffer buffer;
    private int id;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public Producer(String subscriberHost, String publisherHost, int id) {
        buffer = new Buffer(subscriberHost, publisherHost);
        this.id = id;
    }

    @SneakyThrows
    @Override
    public void run() {
        for (int i = 100; i < 120; i++){
            this.buffer.put(i);
            logger.info(id + " Put: " + i);
        }
        logger.info(this.id + " DONE PRODUCING");
    }
}
