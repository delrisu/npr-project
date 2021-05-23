package server;

import lombok.SneakyThrows;
import org.apache.commons.lang3.mutable.MutableBoolean;
import utils.Constants;

import java.util.List;

public class TokenService implements Runnable {

    private final MutableBoolean hasToken;
    private final List<String> messagesToServers;

    public TokenService(MutableBoolean hasToken, List<String> messagesToServers) {
        this.hasToken = hasToken;
        this.messagesToServers = messagesToServers;
    }

    @SneakyThrows
    @Override
    public void run() {
        synchronized (hasToken){
            while (!hasToken.getValue()){
                hasToken.wait();
            }
            hasToken.setFalse();
            messagesToServers.add(Constants.TOKEN);
        }
    }
}
