package adblocker;

public class HttpResponse extends HttpMessage{

    public void appendToHeader(String line) {
        int colon = line.indexOf(":");
        if (!(colon == -1)) {
            String key = line.substring(0,colon);
            String value = line.substring(colon+1);
            super.appendToHeader(key, value);
        } else {
            int code = line.indexOf(" ");
            int codeAfter = line.lastIndexOf(" ");
            super.setProtocolVersion(line.substring(0, code));
            super.setResponseCode(Integer.parseInt(line.substring(code+1, codeAfter)));
            super.setResponseMessage(line.substring(codeAfter));
        }
    }

}
