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

    private String host;
    private String topic;
    private ZMQ.Socket socket;
    private final List<String> receivedMessages;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public Subscriber(String host, ZContext context, List<String> receivedMessages) {
        this.host = host;
        this.socket = context.createSocket(SocketType.SUB);
        socket.connect("tcp://" + host);
        socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
        this.receivedMessages = receivedMessages;
        logger.info("Created subscrber to host: " + host);
    }

    @SneakyThrows
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            synchronized (receivedMessages){
                receivedMessages.add(socket.recvStr());
                logger.info("Received: " + receivedMessages.get(0));
                receivedMessages.notify();
            }
            Thread.sleep(1);
        }
    }
}
