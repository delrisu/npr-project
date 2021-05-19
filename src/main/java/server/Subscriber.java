package server;

import lombok.Data;
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

    public Subscriber(String host, String topic, ZContext context, List<String> receivedMessages) {
        this.host = host;
        this.topic = topic;
        this.socket = context.createSocket(SocketType.SUB);
        socket.connect("tcp://" + host);
        this.receivedMessages = receivedMessages;
    }

    @Override
    public void run() {

    }
}
