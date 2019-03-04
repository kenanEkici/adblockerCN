package adblocker;

public class App {

    public static void main(String[] args) {

        HttpClient client = new HttpClient();
        HttpResponse resp = client.sendRequest(args[0], Integer.parseInt(args[1]), args[2]);
        client.writeToHtml("google.html", resp.getBody());

    }
}
