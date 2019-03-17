package adblocker;

public class Adblocker {

    private String[] blackList;

    protected Adblocker(String[] blackList) {
        this.blackList = blackList;
    }

    /**
     * Cut ad URI from a String according to a blacklist.
     * @param body : String to be cleansed
     * @return : Cleansed String
     */
    protected String snipAds(String body) {
        for (String keyword: blackList) {
            int index = body.indexOf(keyword);
            if (index != -1) {
                body = body.substring(0, index) + "../adblocker/placeholder.png" + body.substring(index+keyword.length());
            }
        }
        return body;
    }
}
