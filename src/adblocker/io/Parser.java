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

            String uri = body.substring(index + 5, nextIndex);
            if (!uri.equals("../adblocker/placeholder.png")) {
                uris.add(uri);
            }
            index = body.indexOf("src=\"", nextIndex);
        }

        return uris;
    }
}
