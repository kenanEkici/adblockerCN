package adblocker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {

    public static void main(String[] args) {

        HttpClient client = new HttpClient();
        HttpResponse resp = client.sendRequest(args[0], Integer.parseInt(args[1]), args[2]);

        try {
            Files.write(Paths.get("websiteOutputs/test.html"), resp.getBody().getBytes());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
