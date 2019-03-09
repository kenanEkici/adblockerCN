package adblocker;

import java.util.ArrayList;

public class Adblocker {

    private String[] blackList;

    Adblocker(String[] blackList) {
        this.blackList = blackList;
    }

    String snipAds(String body) {
        for (String keyword: blackList) {
            int index = body.indexOf(keyword);
            if (index != -1) {
                body = body.substring(0, index) + "../adblocker/placeholder.png" + body.substring(index+keyword.length());
            }
        }
        return body;
    }

    ArrayList<String> cleanseUris(ArrayList<String> uris) {
        for(String keyword: blackList) {
            uris.remove(keyword);
        }
        return uris;
    }
}
