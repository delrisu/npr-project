import producerConsumer.Consumer;
import producerConsumer.Producer;
import server.Server;

public class Program {
    public static void main(String[] args) throws InterruptedException {
        new Server("127.0.0.1:55555", "55556", "55656",
                "55756", 0, "A", true);
        new Server("127.0.0.1:55556", "55557", "55657",
                "55757", 1, "A", false);
        new Server("127.0.0.1:55557", "55558", "55658",
                "55758", 2, "A", false);
        new Server("127.0.0.1:55558", "55559", "55659",
                "55759", 3, "A", false);
        new Server("127.0.0.1:55559", "55560", "55660",
                "55760", 4, "B", true);
        new Server("127.0.0.1:55560", "55555", "55655",
                "55755", 5, "B", false);

        Thread.sleep(2000);


        /////////A
        new Thread(new Producer("127.0.0.1:55756", "127.0.0.1:55656", 661)).start();
        new Thread(new Producer("127.0.0.1:55757", "127.0.0.1:55657", 662)).start();
        new Thread(new Consumer("127.0.0.1:55758", "127.0.0.1:55658", 663)).start();
        new Thread(new Consumer("127.0.0.1:55759", "127.0.0.1:55659", 664)).start();

        ////////B
        new Thread(new Producer("127.0.0.1:55760", "127.0.0.1:55660", 991)).start();
        new Thread(new Consumer("127.0.0.1:55755", "127.0.0.1:55655", 992)).start();
    }
}
