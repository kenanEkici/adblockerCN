package adblocker;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class HttpClient {

    /**
     * Method to send requests to a given socket.
     * @param host : hostname of recipient socket
     * @param route : route to execute method on
     * @param portNumber : port number to connect with host
     * @param httpCommand : (HEAD, GET, POST, PUT)
     */
    protected void sendRequest(String host, String route, int portNumber, String httpCommand) {

        try {

            Socket socket = new Socket(host, portNumber);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            DataInputStream in = new DataInputStream(socket.getInputStream());

            //----SENDING REQUEST

            out.println(httpCommand + " " + route + " " + "HTTP/1.1");
            out.println("Host: " + host + ":" + Integer.toString(portNumber));
            out.println("Connection: Close");
            out.println();

            //----END OF SENDING REQUEST

            HttpResponse resp = new HttpResponse();

            //----READING RESPONSE HEADER

            char c = (char)in.readByte();
            String line = "" + c;

            while (true) {
                while (!line.endsWith("\r\n") ) {
                    c = (char)in.readByte();
                    line += c;
                }
                if (line.equals("\r\n"))
                    break;
                String newLine = line.substring(0,line.indexOf("\r\n"));
                resp.appendToHeader(newLine);
                System.out.println(newLine);
                line = "";
            }

            //----END OF READING RESPONSE HEADER

            System.out.println();

            String contentType = resp.getHeader().get("Content-Type");
            String length = resp.getHeader().get("Content-Length");
            String encoding = resp.getHeader().get("Transfer-Encoding");

            //----READING RESPONSE BODY
            if (resp.getResponseCode() < 400) {
                // HTML RESPONSE
                if (contentType.contains("text/html")) {
                    String body = "";

                    // BY CONTENT LENGTH
                    if (encoding == null) {
                        int len = Integer.parseInt(length);
                        char c2;

                        while (len > 0) {
                            c2 = ((char) in.readByte());
                            body += c2;
                            len--;
                        }
                        System.out.println(body);
                    }
                    // CHUNKED
                    else {
                        body = "" + (char)in.readByte();
                        while (!body.endsWith("\r\n") ) {
                            c = (char)in.readByte();
                            body += c;
                        }
                        System.out.println(body);
                    }

                    // RELEASE ADBLOCK ON BODY
                    Adblocker blocker = new Adblocker(new String[]{"ad1.jpg", "ad2.jpg", "ad3.jpg"});
                    String newBody = blocker.snipAds(body);
                    resp.setBody(newBody);

                    // WRITE NO AD RESULT TO HTML
                    writeToHtml(newBody);

                    // DOWNLOAD UNDERLYING IMAGES RECURSIVELY
                    for (String cleanUri : findImageSourceList(newBody)) {
                        if (!cleanUri.equals("../adblocker/placeholder.png"))
                            sendRequest(host, route + cleanUri, portNumber, "GET");
                    }
                }
                // IMAGE RESPONSE
                else {
                    // CHUNKED
                    if (encoding != null) {
                        if (encoding.equals("chunked")) {
                            System.out.println("A file is being written by chunks");
                            writeToFile(in, route, 0, true);
                        }
                    }
                    // BY CONTENT LENGTH
                    else {
                        System.out.println("A file is being written by content length");
                        writeToFile(in, route, Integer.parseInt(length), false);
                    }
                }
            }

            //----END OF READING RESPONSE BODY

            System.out.println();

            out.close();
            in.close();
            socket.close();

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Algorithm to find the source of image tags in a String.
     * @param body : possibly contains the uri's
     * @return list of found uri's
     */
    private ArrayList<String> findImageSourceList(String body) {
        ArrayList<String> uris = new ArrayList<>();

        int index = body.indexOf("src=\"");
        while (index != -1) {
            int nextIndex = body.indexOf("\"", index+5);
            uris.add(body.substring(index+5,nextIndex));
            index = body.indexOf("src=\"", nextIndex);
        }

        return uris;
    }

    /**
     * Method to write a String to an HTML file.
     * @param body : the String to be written
     */
    private void writeToHtml(String body) {
        try {
            Files.write(Paths.get("websiteOutputs/index.html"), body.getBytes());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void writeToFile(DataInputStream in, String fileName, int contentLength, boolean isChunked) throws IOException {

        // split actual file and its directory structure
        int lastSlash = fileName.lastIndexOf("/");
        String directories = fileName.substring(0,lastSlash);
        File directory = new File("websiteOutputs/"+directories);

        // create underlying directory structure if it does not exist
        if (! directory.exists()){
            directory.mkdirs();
        }
        OutputStream fileWriter = new FileOutputStream("websiteOutputs/"+fileName);

        //----WRITING BYTES TO FILE

        // BY CONTENT LENGTH
        if (!isChunked) {
            int totalRead = 0;
            byte[] buffer = new byte[8192]; // chunk size
            while (totalRead < contentLength)
            {
                int count = in.read(buffer);
                fileWriter.write(buffer, 0, count);
                totalRead += count;
            }
        }
        // BY CHUNKS
        else {
            int count;
            byte[] buffer = new byte[8192]; // chunk size
            while ((count = in.read(buffer)) > 0) {
                fileWriter.write(buffer, 0, count);
            }
        }

        //----END OF WRITING TO FILE

        fileWriter.close();
    }
}
