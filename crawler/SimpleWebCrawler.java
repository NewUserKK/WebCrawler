package crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.io.*;
import java.util.regex.*;

/**
 * @author Kostin Konstantin
 */
public class SimpleWebCrawler implements WebCrawler {
    private Downloader downloader;
    private HashMap<String, Page> visitedPages;
    private HashMap<String, Image> visitedImages;

    SimpleWebCrawler(final Downloader downloader) throws IOException {
        this.downloader = downloader;
        this.visitedPages = new HashMap<>();
        this.visitedImages = new HashMap<>();
    }

    public Page crawl(String url, int depth) {
        if (depth == 0) {
            return new Page(url, "");
        }
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(downloader.download(url), "utf-8"))) {
            Page page = null;
            String title;
            StringBuilder sbDoc = new StringBuilder();
            while (in.ready()) {
                sbDoc.append(in.readLine().trim().replaceAll("\n", ""));
            }
            String doc = replaceEntities(sbDoc.toString());
            Pattern titlePattern = Pattern.compile("<title>.*</title>");
            Matcher titleMatcher = titlePattern.matcher(doc);
            if (titleMatcher.find()) {
                String titleGroup = titleMatcher.group();
                title = titleGroup.substring(7, titleGroup.length() - 8);
                page = new Page(url, title);
            }
            doc = doc.replaceAll("<!--.*?-->", "");
            if (!visitedPages.containsKey(url)) {
                visitedPages.put(url, page);
            }
            parseLinks(doc, page, depth);
            parseImages(doc, page, "base_cache.zip");

            return page;

        } catch (IOException e) {
            //e.printStackTrace();
            return new Page(url, "");
        }
    }

    private void parseLinks(String currLine, Page page, int depth) {
        Pattern linkPattern = Pattern.compile("<a.*?>"); //извлечение тега
        Matcher tagLinkMatcher = linkPattern.matcher(currLine);
        while (tagLinkMatcher.find()) {
            String link = extractLink(tagLinkMatcher.group(), page); //ссылка
            if (link == null) {
                continue;
            }
            Page currPage; //страница, которая попалась в текущей строке
            if (visitedPages.containsKey(link)) {
                currPage = visitedPages.get(link);
            } else {
                currPage = crawl(link, depth - 1);
                if (currPage.getUrl().equals(page.getUrl())) {
                    currPage = page;
                }
                visitedPages.put(currPage.getUrl(), currPage);

            }
            page.addLink(currPage);
        }
    }

    private String extractLink(String linkTag, Page page) {
        Pattern hrefAttrPattern = Pattern.compile("href\\p{Space}*=\\p{Space}*\".*?\""); //уровень аттрибута
        Matcher hrefAttrMatcher = hrefAttrPattern.matcher(linkTag);
        if (hrefAttrMatcher.find()) {
            String href = hrefAttrMatcher.group();
            Pattern linkPattern = Pattern.compile("\".*\""); //уровень ссылки
            Matcher linkMatcher = linkPattern.matcher(href);
            String link = "";
            if (linkMatcher.find()) {
                link = linkMatcher.group();
                link = link.substring(1, link.length() - 1).trim();
            }
            return toAbsoluteLink(link, page);
        }
        return null;
    }

    private void parseImages(String currLine, Page page, String zipPath) throws IOException {
        Pattern imgPattern = Pattern.compile("<img.*?>");
        Matcher tagImgMatcher = imgPattern.matcher(currLine);
        while (tagImgMatcher.find()) {
            String link = tagImgMatcher.group();
            Image image = extractImage(link, page, zipPath);
            if (image == null) {
                continue;
            }
            if (!visitedImages.containsKey(image.getFile())) {
                visitedImages.put(image.getFile(), image);
            } else {
                image = visitedImages.get(image.getFile());
            }
            page.addImage(image);
        }
    }

    private Image extractImage(String imageTag, Page page, String zipPath) throws IOException {
        Pattern srcLinkPattern = Pattern.compile("src *= *\".*?\""); //аттрибут
        Matcher srcLinkMatcher = srcLinkPattern.matcher(imageTag);
        if (srcLinkMatcher.find()) {
            String src = srcLinkMatcher.group();
            Pattern linkPattern = Pattern.compile("\".*\""); //ссылка
            Matcher linkMatcher = linkPattern.matcher(src);
            String link = "";
            if (linkMatcher.find()) {
                link = linkMatcher.group();
                link = link.substring(1, link.length() - 1).trim();
            }
            String imgLink = toAbsoluteLink(link, page);
            String imgFile = CachingDownloader.encode(imgLink);
            Downloader imgDownloader = new CachingDownloader(
                    new ZipDownloader(zipPath), "images");
            imgDownloader.download(imgLink);
            return new Image(imgLink, String.format("images\\%s", imgFile));
        }
        return null;
    }

    public static String toAbsoluteLink(String relativeLink) {
        if (!relativeLink.startsWith("http://") || !relativeLink.startsWith("https://")) {
            return String.format("http://%s", relativeLink);
        }
        return relativeLink;
    }

    private String toAbsoluteLink(String relativeLink, Page page) {
        URL url;
        URL absLink = null;
        try {
            url = new URL(page.getUrl());
            absLink = new URL(url, relativeLink);
        } catch (MalformedURLException e) {
            System.err.print("No url found");
        }
        return absLink.toString().replaceAll("#.*", "");
    }

    private static String replaceEntities(String s) {
        return s.replaceAll("(&lt;?)", "<")
                .replaceAll("(&gt;?)", ">")
                .replaceAll("(&amp;?)", "&")
                .replaceAll("&mdash;", "\u2014")
                .replaceAll("&nbsp;", "\u00A0");
    }
}
