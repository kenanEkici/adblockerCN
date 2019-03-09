package adblocker;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class HttpClient {

    void sendRequest(String root, String route, int portNumber, String httpCommand) {
        try {

            Socket socket = new Socket(root, portNumber);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            DataInputStream in = new DataInputStream(socket.getInputStream());

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
                System.out.println(line);
                line = in.readLine();
            }

            String contentType = resp.getHeader().get("Content-Type");

            if (contentType.contains("text/html")) {
                String body = "";
                while ((line = in.readLine()) != null) {
                    body += line;
                    System.out.println(line);
                }
                //write adless html to file
                Adblocker blocker = new Adblocker(new String[]{"ad1.jpg","ad2.jpg", "ad3.jpg"});
                String newBody = blocker.snipAds(body);
                resp.setBody(newBody);
                writeToHtml(newBody);

                ArrayList<String> uriList = findImageSourceList(body);

                //avoid downloading ads
                for(String cleanUri: blocker.cleanseUris(uriList) ) {
                    sendRequest(root, route+cleanUri, portNumber, "GET");
                }

            } else {
                System.out.println("A file is being written");
                writeToFile(in, route);
            }

            out.close();
            in.close();
            socket.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private ArrayList<String> findImageSourceList(String body) {
        ArrayList<String> images = new ArrayList<>();

        int index = body.indexOf("src=\"");
        while (index != -1) {
            int nextIndex = body.indexOf("\"", index+5);
            images.add(body.substring(index+5,nextIndex));
            index = body.indexOf("src=\"", nextIndex);
        }

        return images;
    }

    private void writeToHtml(String body) {
        try {
            Files.write(Paths.get("websiteOutputs/index.html"), body.getBytes());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void writeToFile(DataInputStream in, String fileName) throws IOException {
        int lastSlash = fileName.lastIndexOf("/");
        String directories = fileName.substring(0,lastSlash);
        File directory = new File("websiteOutputs/"+directories);
        if (! directory.exists()){
            directory.mkdirs();
        }
        OutputStream fileWriter = new FileOutputStream("websiteOutputs/"+fileName);

        int count;
        byte[] buffer = new byte[8192]; // or 4096, or more
        while ((count = in.read(buffer)) > 0)
        {
            fileWriter.write(buffer, 0, count);
        }

        fileWriter.close();
    }
}
