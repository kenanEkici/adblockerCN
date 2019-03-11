package adblocker;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

class HttpClient {

    void sendRequest(String host, String route, int portNumber, String httpCommand) {
        try {

            Socket socket = new Socket(host, portNumber);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // send request
            out.println(httpCommand + " " + route + " " + "HTTP/1.1");
            out.println("Host: " + host + ":" + Integer.toString(portNumber));
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
            String length = resp.getHeader().get("Content-Length");
            String encoding = resp.getHeader().get("Transfer-Encoding");

            if (resp.getResponseCode() < 400) {
                if (contentType.contains("text/html")) {
                    String body = "";

                    // if content length is available, read byte per byte
                    if (length != null) {
                        int len = Integer.parseInt(length);
                        char c;

                        while (len > 0) {
                            c = ((char) in.readByte());
                            body += c;
                            len--;
                        }
                        System.out.println(body);
                    }
                    //otherwise we rely on chunked transfer encoding
                    else {
                        while ((line = in.readLine()) != null) {
                            body += line;
                            System.out.println(line);
                        }
                    }

                    //write adless html to file
                    Adblocker blocker = new Adblocker(new String[]{"ad1.jpg", "ad2.jpg", "ad3.jpg"});
                    String newBody = blocker.snipAds(body);
                    resp.setBody(newBody);
                    writeToHtml(newBody);

                    ArrayList<String> uriList = findImageSourceList(body);

                    //avoid downloading ads
                    //TODO just check if placeholder image or not
                    for (String cleanUri : blocker.cleanseUris(uriList)) {
                        sendRequest(host, route + cleanUri, portNumber, "GET");
                    }
                } else {
                    // write by chunks
                    if (length == null && encoding != null) {
                        if (encoding.equals("chunked")) {
                            System.out.println("A file is being written by chunks");
                            writeToFile(in, route, 0, true);
                        }
                    }
                    // write by content length
                    else {
                        System.out.println("A file is being written by content length");
                        writeToFile(in, route, Integer.parseInt(length), false);
                    }
                }
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

    private void writeToFile(DataInputStream in, String fileName, int contentLength, boolean isChunked) throws IOException {
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
