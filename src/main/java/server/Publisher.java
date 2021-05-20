package server;

import lombok.Data;
import lombok.SneakyThrows;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.List;

@Data
public class Publisher implements Runnable {

    private ZMQ.Socket socket;
    private List<String> messagesToSend;

    Publisher(String port, ZContext context, List<String> messages) {
        socket = context.createSocket(SocketType.PUB);
        socket.bind("tcp://*:" + port);
        this.messagesToSend = messages;
    }

    @SneakyThrows
    @Override
    public void run() {

        synchronized (ServerCommunication.publisherMonitor) {
            while (messagesToSend.size() == 0) {
                ServerCommunication.publisherMonitor.wait();
            }
            messagesToSend.forEach(message -> socket.send(message));
            ServerCommunication.publisherMonitor.notify();
        }

    }
}
