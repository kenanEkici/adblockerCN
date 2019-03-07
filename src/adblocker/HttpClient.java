package adblocker;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HttpClient {

    private Adblocker blocker;

    public HttpResponse sendRequest(String root, String route, int portNumber, String httpCommand) {
        try {

            Socket socket = new Socket(root, portNumber);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // send request
            out.println(httpCommand + " " + route + " " + "HTTP/1.1");
            out.println("Host: " + root + ":" + Integer.toString(portNumber));
            out.println("Connection: Close");
            out.println();

            HttpResponse resp = new HttpResponse();

            // header
            String line = in.readLine();
            while (line != null && !line.equals("")) {
                resp.appendToHeader(line);
                line = in.readLine();
            }

            String contentType = resp.getHeader().get("Content-Type");

            if (contentType.equals("text/html")) {
                String body = in.readLine();
                resp.setBody(body);
                writeToHtml("test.html", body);

                for(String uri: findImageSourceList(body)){
                    sendRequest(root, route+uri, portNumber, "GET");
                }
            }

            if (contentType.equals("image/jpg")) {
                InputStream in2 = socket.getInputStream();
                OutputStream out2 = new FileOutputStream("websiteOutputs/"+route);

                out2.write(in2.readAllBytes());

                out2.close();
                in2.close();

            }

            socket.close();
            out.close();
            in.close();

            return resp;

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    public ArrayList<String> findImageSourceList(String body) {
        ArrayList<String> images = new ArrayList<>();

        int index = body.indexOf("src=\"");
        while (index != -1) {
            int nextIndex = body.indexOf("\"", index+5);
            images.add(body.substring(index+5,nextIndex));
            index = body.indexOf("src=\"", nextIndex);
        }

        return images;
    }

    private void writeToHtml(String filename, String body) {
        try {
            Files.write(Paths.get("websiteOutputs/"+filename), body.getBytes());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
