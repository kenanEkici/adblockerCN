package adblocker.clientServer;

import adblocker.Adblocker;
import adblocker.httpMessages.HttpResponse;
import adblocker.io.ByteReader;
import adblocker.io.ByteWriter;
import adblocker.io.Parser;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class HttpClient {

    /**
     * Entry point for client
     * @param args : HOSTNAME, ROUTE, PORT NUMBER, HTTP COMMAND
     */
    public static void main(String[] args) {
        HttpClient client = new HttpClient();
        client.sendRequest(args[0], args[1], Integer.parseInt(args[2]), args[3]);
    }

    /**
     * Method to send requests to a given socket.
     *
     * @param host        : hostname of recipient socket
     * @param route       : route to execute method on
     * @param portNumber  : port number to connect with host
     * @param httpCommand : (HEAD, GET, POST, PUT)
     */
    private void sendRequest(String host, String route, int portNumber, String httpCommand) {
        try {
            switch (httpCommand) {
                case "GET":
                    sendHeadOrGetRequest(host,route, portNumber, httpCommand, false);
                    break;
                case "HEAD":
                    sendHeadOrGetRequest(host, route, portNumber, httpCommand, true);
                    break;
                case "PUT":
                case "POST":
                    sendPutOrPostRequest(host, route, portNumber, httpCommand);
                    break;
                default:
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Method to send HEAD and GET requests to a given socket.
     *
     * @param host        : hostname of recipient socket
     * @param route       : route to execute method on
     * @param portNumber  : port number to connect with host
     * @param httpCommand : (HEAD, GET)
     */
    private void sendHeadOrGetRequest(String host, String route, int portNumber, String httpCommand, boolean isHead) throws IOException {
        Socket socket = new Socket(host, portNumber);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        // local reader/writers
        ByteReader bReader = new ByteReader();
        ByteWriter bWriter = new ByteWriter();
        Parser parser = new Parser();

        //----SENDING REQUEST
        handleHeaderGetOrHead(out, host, route, portNumber, httpCommand, !isHead);

        //----READING RESPONSE HEADER
        HttpResponse resp = new HttpResponse();
        handleResponseHeader(in, resp);

        //----READING RESPONSE BODY

        if (resp.getResponseCode() < 400 && !isHead) {

            String contentType = resp.getHeader().get("Content-Type");
            String length = resp.getHeader().get("Content-Length");
            String encoding = resp.getHeader().get("Transfer-Encoding");

            if (contentType.contains("text/html")) {

                // read HTML body from stream
                String body;

                // BY CONTENT LENGTH
                if (encoding == null)
                    body = bReader.readBodyByLength(in, length);
                // BY CHUNKS
                else
                    body = bReader.readBodyByChunks(in);

                // RELEASE ADBLOCK ON BODY
                Adblocker blocker = new Adblocker(new String[]{"ad1.jpg", "ad2.jpg", "ad3.jpg"});
                String newBody = blocker.snipAds(body);
                System.out.println(newBody);
                resp.setBody(newBody);
                System.out.println();


                // WRITE NO AD RESULT TO HTML
                bWriter.writeToHtml(newBody);

                ArrayList<String> uris = parser.findImageSourceList(newBody);

                // DOWNLOAD UNDERLYING IMAGES WITH SAME CONNECTION
                for (int i = 0; i < uris.size(); i++) {
                    String cleanUri = uris.get(i);
                    boolean isLastFile = i == uris.size() - 1;
                    //---- SENDING REQUEST
                    handleHeaderGetOrHead(out, host, route + cleanUri, portNumber, httpCommand, !isLastFile);

                    // KEEP CLIENT ALIVE
                    String line = in.readLine();

                    //---- READING RESPONSE HEADER
                    HttpResponse imageResp = new HttpResponse();
                    handleResponseHeader(in, imageResp);

                    contentType = imageResp.getHeader().get("Content-Type");
                    length = imageResp.getHeader().get("Content-Length");
                    encoding = imageResp.getHeader().get("Transfer-Encoding");

                    //---- READING RESPONSE BODY
                    if (contentType.contains("image")) {

                        System.out.println();

                        // CHUNKED
                        if (encoding != null) {
                            if (encoding.equals("chunked")) {
                                System.out.println("A file is being written by chunks");
                                bWriter.writeToFileByChunks(in, route + cleanUri);
                            }
                        }

                        // BY CONTENT LENGTH
                        else {
                            System.out.println("A file is being written by content length");
                            bWriter.writeToFileByLength(in, route + cleanUri, Integer.parseInt(length));
                        }
                    }
                    System.out.println();
                }
            }
        }

        out.close();
        in.close();
        socket.close();
    }

    /**
     * Method to send Put and Post requests to a given socket.
     *
     * @param host        : hostname of recipient socket
     * @param route       : route to execute method on
     * @param portNumber  : port number to connect with host
     * @param httpCommand : (POST, PUT)
     */
    private void sendPutOrPostRequest(String host, String route, int portNumber, String httpCommand) throws IOException {
        Socket socket = new Socket(host, portNumber);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        //----SENDING REQUEST
        handleHeaderPutOrPost(out, host, route, portNumber, httpCommand);

        //----READING RESPONSE HEADER
        HttpResponse resp = new HttpResponse();
        handleResponseHeader(in, resp);

        System.out.println();

        //----PROCESS RESPONSE
        if (resp.getResponseCode() == 200) {
            System.out.println(httpCommand + "-Command successfully executed!");
        } else {
            System.out.println("Error: " + resp.getResponseCode() + ": " + resp.getResponseMessage());
        }

        out.close();
        in.close();
        socket.close();
    }

    /**
     * Sends the headers to establish a HTTP connection with the server
     * @param out : Stream to write bytes to
     * @param host : Hostname server
     * @param route : Route to request
     * @param portNumber : Port number to access server
     * @param httpCommand : Type of request
     * @param keepAlive : Whether to reuse socket and keep connection alive
     * @throws IOException IOException
     */
    private void handleHeaderGetOrHead(DataOutputStream out, String host, String route, int portNumber, String httpCommand, boolean keepAlive) throws IOException {
        out.writeBytes(httpCommand + " " + route + " " + "HTTP/1.1" + "\r\n");
        out.writeBytes("Host: " + host +  ":" + Integer.toString(portNumber) + "\r\n");

        if (keepAlive) {
            out.writeBytes("Connection: Keep Alive" + "\r\n");
        } else {
            out.writeBytes("Connection: Close" + "\r\n");
        }
        out.writeBytes("\r\n");
    }

    /**
     * Sends the headers to establish a HTTP connection with the server
     * @param out : Stream to write bytes to
     * @param host : Hostname server
     * @param route : Route to request
     * @param portNumber : Port number to access server
     * @param httpCommand : Type of request
     * @throws IOException IOException
     */
    private void handleHeaderPutOrPost(DataOutputStream out, String host, String route, int portNumber, String httpCommand) throws IOException {
        System.out.println("Please input your message to the server");
        Scanner scanner = new Scanner(System.in);
        String content = scanner.nextLine();
        scanner.close();

        out.writeBytes(httpCommand + " " + route + " " + "HTTP/1.1" + "\r\n");
        out.writeBytes("Host: " + host + ":" + Integer.toString(portNumber) + "\r\n");
        out.writeBytes("Content-Type: text/plain" + "\r\n");
        out.writeBytes("Content-Length: " + content.getBytes().length + "\r\n");
        out.writeBytes("Connection: Close"+"\r\n");
        out.writeBytes("\r\n");
        out.writeBytes(content+"\r\n");
    }

    /**
     * Reads the headers from the server response and wraps them in HttpResponse
     * @param in : Stream to read header bytes from
     * @param resp : Response to which to encapsulate data into
     * @throws IOException : IOException
     */
    public void handleResponseHeader(DataInputStream in, HttpResponse resp) throws IOException {
        char c = (char) in.readByte();
        String line = "" + c;

        while (true) {
            while (!line.endsWith("\r\n")) {
                c = (char) in.readByte();
                line += c;
            }
            if (line.equals("\r\n"))
                break;
            String newLine = line.substring(0, line.indexOf("\r\n"));
            resp.appendToHeader(newLine);
            System.out.println(newLine);
            line = "";
        }
    }
}
