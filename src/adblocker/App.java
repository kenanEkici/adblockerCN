package adblocker;

public class App {

    public static void main(String[] args) {

        // run HTTP server on a thread
        HttpServer webServer = new HttpServer();
        Thread t = new Thread(webServer);
        t.start();

        // create client and send request
        HttpClient client = new HttpClient();
        HttpMessage resp = client.sendRequest(args[0], Integer.parseInt(args[1]), args[2]);

        if (resp != null) {
            client.writeToHtml("test.html", resp.getBody());
        }
    }
}
