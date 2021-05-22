package server;

import lombok.SneakyThrows;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Constants;

import java.util.List;

public class Initiator implements Runnable{

    private MutableBoolean initialized;
    private List<String> messagesToSend;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public Initiator(MutableBoolean initialized, List<String> messagesToSend) {
        this.initialized = initialized;
        this.messagesToSend = messagesToSend;
    }

    @SneakyThrows
    @Override
    public void run() {
        while(this.initialized.getValue() == false){
            synchronized (messagesToSend) {
                messagesToSend.add(Constants.INITIALIZE_MESSAGE);
                logger.info("LEADER Sent INIT");
                messagesToSend.notify();
            }
            Thread.sleep(100);
        }

    }
}
