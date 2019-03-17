package adblocker.io;

import java.io.*;

public class ByteReader {

    /**
     * Reads bytes by content length and returns as String body
     * @param in : Stream to read bytes from
     * @return String body of bytes
     * @throws IOException IOException
     */
    public String readBodyByLength(DataInputStream in, String length) throws IOException {
        String body = "";

        int len = Integer.parseInt(length);
        char c2;

        while (len > 0) {
            c2 = ((char) in.readByte());
            body += c2;
            len--;
        }
        return body;
    }

    /**
     * Reads bytes by chunks and returns as String body
     * @param in : Stream to read bytes from
     * @return String body of bytes
     * @throws IOException IOException
     */
    public String readBodyByChunks(DataInputStream in) throws IOException {
        String body = "";
        while (true) {

            String chunkLength = "";
            chunkLength += (char) in.readByte();
            while (!chunkLength.contains("\r\n"))
                chunkLength += (char) in.readByte();

            if (chunkLength == null)
                break;

            byte[] buffer;// chunk size
            int chunksize = Integer.parseInt(chunkLength.substring(0, chunkLength.length() - 2), 16);
            if (chunksize == 0)
                break;
            buffer = new byte[chunksize];

            in.read(buffer);
            String line = new String(buffer);
            body += line;

            String newLine = "";
            newLine += (char) in.readByte();
            while (!newLine.contains("\r\n"))
                newLine += (char) in.readByte();
        }
        return body;
    }
}
