import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xuanwang on 12/24/16.
 */
public class PodcastsClient {
    private String url;
    private String rssEntryPoint;

    public PodcastsClient(String url){
        this.url = url;
        this.rssEntryPoint = this.url + "/rss/";
    }

    public void getRssFeed(String id, int numOfFeeds) throws IOException {
        String rssUrl = this.rssEntryPoint + id + ".xml";

        URL feed = new URL(rssUrl);
        HttpURLConnection con = (HttpURLConnection) feed.openConnection();
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        if(responseCode != 200){
            return;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while((inputLine = in.readLine()) != null){
            response.append(inputLine);
        }
        String xml = response.toString();

        JSONObject json = XML.toJSONObject(xml);

        JSONArray items = json.getJSONObject("rss").getJSONObject("channel").getJSONArray("item");

        ExecutorService executorService = Executors.newCachedThreadPool();

        for(int i = 0; i < Math.min(items.length(), numOfFeeds); i++){
            JSONObject item = items.getJSONObject(i);
            String guid = item.getString("guid");
            String title = item.getString("title");
            executorService.submit(new DownloadTask(guid, title));
        }

        System.out.println(response.toString());
    }


    private static class DownloadTask implements Runnable {

        private String url;
        private final String title;

        public DownloadTask(String url, String title) {
            this.url = url;
            this.title = title;

        }

        private void doDownload(String url, String title) throws IOException {
            String[] tokens = url.split("\\.");
            String fileName = title.replace('/', ' ').replace('.', ' ').replace(' ', '_') + "." + tokens[tokens.length-1];
            File f = new File(fileName);
            if(f.exists() && !f.isDirectory()) {
                // do something
                return;
            }
            URL audioURL = new URL(url);

            InputStream in = audioURL.openStream();
            FileOutputStream fos = new FileOutputStream(fileName);
            int length = -1;
            byte[] buffer = new byte[1024];// buffer for portion of data from
            // connection
            while ((length = in.read(buffer)) > -1) {
                fos.write(buffer, 0, length);
            }

            fos.close();
            in.close();
        }

        public void run() {
            // surround with try-catch if downloadFile() throws something
            try {
                doDownload(url, title);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        PodcastsClient client = new PodcastsClient("http://www.lizhi.fm");
        try {
            client.getRssFeed("1307862", 10);
        }catch(IOException e){
            System.out.println(e);
        }
        System.out.println("Done");
    }

}
