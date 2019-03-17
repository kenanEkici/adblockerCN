package adblocker;

public class App {

    /**
     * Entry point for HTTP client
     * @param args : HOSTNAME, ROUTE, PORT NUMBER, HTTP METHOD
     */
    public static void main(String[] args) {
        HttpClient client = new HttpClient();
        client.sendRequest(args[0], args[1], Integer.parseInt(args[2]), args[3]);
    }
}
