package adblocker.httpMessages;

import java.util.HashMap;
import java.util.Map;

public abstract class HttpMessage {

    private Map<String, String> header = new HashMap<>();
    private String body;
    private String protocolVersion = "HTTP/1.1";

    public Map<String, String> getHeader() {
        return header;
    }

    public abstract void appendToHeader(String line);

    public void appendToHeader(String key, String value) {
        this.header.put(key, value);
    }

    public String getBody() { return body; }

    public void setBody(String body) {
        this.body = body;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

}
