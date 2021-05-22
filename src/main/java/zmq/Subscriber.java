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
    private List<String> receivedMessages;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public Subscriber(String host, String topic, ZContext context, List<String> receivedMessages) {
        this.host = host;
        this.topic = topic;
        this.socket = context.createSocket(SocketType.SUB);
        logger.info(host);
        socket.connect("tcp://" + host);
        socket.subscribe(topic);
        this.receivedMessages = receivedMessages;
    }

    @SneakyThrows
    @Override
    public void run() {
        logger.info("Running!");
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
