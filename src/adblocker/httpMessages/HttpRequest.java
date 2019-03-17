package adblocker.httpMessages;

public class HttpRequest extends HttpMessage {

    private String httpMethod;
    private String route;

    @Override
    public void appendToHeader(String line) {
        int colon = line.indexOf(":");
        if (!(colon == -1)) {
            String key = line.substring(0,colon);
            String value = line.substring(colon+1);
            super.appendToHeader(key.trim(), value.trim());
        } else {
            int firstSpace = line.indexOf(" ");
            int secondSpace = line.indexOf(" ", firstSpace+1);

            setHttpMethod(line.substring(0, firstSpace));
            setHttpRoute(line.substring(firstSpace+1, secondSpace));
            setProtocolVersion(line.substring(secondSpace+1));
        }
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setHttpRoute(String route) {
        this.route = route;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRoute() {
        return route;
    }

}
