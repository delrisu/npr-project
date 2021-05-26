package zmq;

import lombok.Data;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.List;

@Data
public class Subscriber implements Runnable {

    private final List<String> receivedMessages;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private String host;
    private String topic;
    private ZMQ.Socket socket;

    public Subscriber(String argument, ZContext context, List<String> receivedMessages, boolean connect) {
        this.host = argument;
        this.socket = context.createSocket(SocketType.SUB);
        if(connect) {
            socket.connect("tcp://" + argument);
            logger.info("Created subscrber to host: " + argument);
        } else {
            socket.bind("tcp://*:" + argument);
            logger.info("Created subscrber to port: " + argument);
        }
        socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
        this.receivedMessages = receivedMessages;
    }

    @SneakyThrows
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String message = socket.recvStr();
            synchronized (receivedMessages) {
                receivedMessages.add(message);
                logger.info("Received: " + receivedMessages.get(0));
                receivedMessages.notify();
            }
            Thread.sleep(5);
        }
        this.socket.close();
    }
}
