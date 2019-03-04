package adblocker;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class HttpClient {

    public HttpResponse sendRequest(String hostName, int portNumber, String httpCommand) {
        try {
            InetAddress addr = InetAddress.getByName(hostName);
            Socket socket = new Socket(addr, portNumber);

            boolean autoflush = true;
            PrintWriter out = new PrintWriter(socket.getOutputStream(), autoflush);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(httpCommand + " / HTTP/1.1");
            out.println("Host: " + hostName + ":" + Integer.toString(portNumber));
            out.println("Connection: Close");
            out.println();

            HttpResponse resp = new HttpResponse();

            String line = in.readLine();
            while (line != null && !line.equals("")) {
                resp.appendToHeader(line);
                line = in.readLine();
            }

            String body = in.readLine();
            resp.setBody(body);
            socket.close();
            return resp;

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    private void downloadImage(String root, String uri) {

    }

    private void downloadImages(ArrayList<String> imageUris) {

    }

    private void identifyImages(String body) {

    }

    public void writeToHtml(String filename, String body) {
        try {
            Files.write(Paths.get("websiteOutputs/"+filename), body.getBytes());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
