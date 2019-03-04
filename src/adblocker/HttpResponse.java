package adblocker;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private String protocolVersion;
    private int responseCode;
    private String responseMessage;

    private Map<String, String> header = new HashMap<>();
    private String body;

    public Map<String, String> getHeader() {
        return header;
    }

    public void appendToHeader(String line) {
        int colon = line.indexOf(":");
        if (!(colon == -1)) {
            String key = line.substring(0,colon);
            String value = line.substring(colon+1);
            header.put(key, value);
        } else {
            int code = line.indexOf(" ");
            int codeAfter = line.lastIndexOf(" ");
            this.protocolVersion = line.substring(0, code);
            this.responseCode = Integer.parseInt(line.substring(code+1, codeAfter));
            this.responseMessage = line.substring(codeAfter);
        }
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
