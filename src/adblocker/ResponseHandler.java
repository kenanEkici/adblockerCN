package adblocker;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;

public class ResponseHandler implements Runnable {

    private Socket client;

    public ResponseHandler(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            BufferedReader clientRequest = new BufferedReader(new InputStreamReader(client.getInputStream()));

            // this is where we get our header information
            String line = clientRequest.readLine();

            // this is the stream we use for sending our reponse to the client
            DataOutputStream clientResponse = new DataOutputStream(client.getOutputStream());

            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();


            BufferedReader in = new BufferedReader(new FileReader("public/webpage.html"));
            String body = in.lines().collect(Collectors.joining());
            in.close();

            //header
            clientResponse.writeBytes("HTTP/1.0 200 OK\r\n");
            clientResponse.writeBytes("Date: " + dateFormat.format(date) + "\r\n");
            clientResponse.writeBytes("Content-Type: text/html\r\n");
            clientResponse.writeBytes("Content-Length: " + body.length() + "\r\n");
            clientResponse.writeBytes("\n");

            //body
            clientResponse.writeBytes(body);
            clientResponse.writeBytes("\n");

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
