package adblocker.io;

import adblocker.httpMessages.HttpResponse;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Parser {

    /**
     * Algorithm to find the source of image tags in a String.
     *
     * @param body : possibly contains the uri's
     * @return list of found uri's
     */
    public ArrayList<String> findImageSourceList(String body) {
        ArrayList<String> uris = new ArrayList<>();

        int index = body.indexOf("src=\"");
        while (index != -1) {
            int nextIndex = body.indexOf("\"", index + 5);
            uris.add(body.substring(index + 5, nextIndex));
            index = body.indexOf("src=\"", nextIndex);
        }

        return uris;
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
