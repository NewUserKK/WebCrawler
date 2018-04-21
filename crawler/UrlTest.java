package crawler;

import java.net.URL;
import java.util.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kostin Konstantin
 */
public class UrlTest {

    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in));
        System.out.print("Enter url:\n>> ");
        String url = SimpleWebCrawler.toAbsoluteLink(in.readLine());
        Page page = new SimpleWebCrawler(new UrlDownloader()).crawl(url, 1);
        System.out.println(page.toString());
        in.close();
    }
}
