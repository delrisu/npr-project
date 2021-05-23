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
public class Publisher implements Runnable {

    private final List<String> messagesToSend;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private ZMQ.Socket socket;

    public Publisher(String port, ZContext context, List<String> messages) {
        socket = context.createSocket(SocketType.PUB);
        socket.bind("tcp://*:" + port);
        logger.info("Created publisher with port: " + port);
        this.messagesToSend = messages;
    }

    @SneakyThrows
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (messagesToSend) {
                while (messagesToSend.size() == 0) {
                    messagesToSend.wait();
                }
                messagesToSend.forEach(message -> {
                    socket.send(message);
                    logger.info("Sent: " + message);
                });
                messagesToSend.clear();
                messagesToSend.notify();
            }
        }
    }
}
