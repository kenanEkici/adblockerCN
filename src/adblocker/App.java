package adblocker;

public class App {

    public static void main(String[] args) throws InterruptedException {

        // create client and send request
        HttpClient client = new HttpClient();
        client.sendRequest(args[0], args[1], Integer.parseInt(args[2]), args[3]);
    }
}
