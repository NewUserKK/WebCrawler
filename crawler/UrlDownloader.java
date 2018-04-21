package crawler;

import java.net.URL;
import java.util.*;
import java.io.*;

/**
 * @author Kostin Konstantin
 */

public class UrlDownloader implements Downloader {

    public InputStream download(String url) throws IOException {
        URL currUrl = new URL(url);
        return currUrl.openStream();
    }

}
