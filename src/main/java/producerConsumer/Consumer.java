package producerConsumer;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer implements Runnable{

    private final Buffer buffer;
    private int id;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public Consumer(String subscriberHost, String publisherHost, int id) {
        this.buffer = new Buffer(subscriberHost, publisherHost);
        this.id = id;
    }

    @SneakyThrows
    @Override
    public void run() {
        for (int i = 100; i < 120; i++){
            logger.info(id + " Get: " + buffer.get());
        }
        logger.info(this.id + " DONE CONSUMING");
    }
}
