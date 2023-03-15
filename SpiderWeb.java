import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SpiderWeb {

    /*
     * @urlList -> list of valid urls discovered through a level-order
     *               traversal of the html doc
     * @url -> url being checked for
     * 
     * returns true if the urlList already contains the url
     *  returns false otherwise
     */
    public static boolean contains(String[] urlList, String url) {
        int i = 0;
        while (i < urlList.length && urlList[i] != null) {
            if (urlList[i].equals(url)) {
                return true;
            }
            i++;
        }
        return false;
    }

    /*
     * @urlList -> list of valid urls discovered through a level-order
     *               traversal of the html doc
     * @ind1 -> index of first element to be swapped
     * @ind2 -> index of second element to be swapped
     * 
     * swaps elements of urlList at indexes ind1 and ind2
     */
    public static void swap(String[] urlList, int ind1, int ind2) {
        String temp = urlList[ind1];
        urlList[ind1] = urlList[ind2];
        urlList[ind2] = temp;
    }

    /*
     * @urlList -> list of valid urls discovered through a level-order
     *               traversal of the html doc
     * 
     * performs insertion sort on the urlList to put the urls in
     *  alphabetical order
     */
    public static String[] sort(String[] urlList) {
        int size = 0;
        while (size < urlList.length && urlList[size] != null) {
            size++;
        }

        int i = 1;
        while (i < size - 1) {
            int j = i;
            while (j > 0 && urlList[j - 1].compareTo(urlList[j]) > 0) {
                swap(urlList, j, j - 1);
                j--;
            }
            i++;
        }
        return urlList;
    }

    /*
     * @urlList -> list of valid urls discovered through a level-order
     *               traversal of the html doc
     * 
     * just prints @urlList, the formatting is already done
     */
    public static void print(String[] urlList) {
        int i = 0;
        while (i < urlList.length && urlList[i] != null) {
            System.out.println(urlList[i]);
            i++;
        }
    }

    /*
     * @queries -> list of queries from a url, split by '&'
     * 
     * returns a sorted list of url queries, sorted by name, and then
     *  by value if the names are equal
     */
    public static ArrayList<String> buildQueryList(String[] queries) {
        ArrayList<String> sortedQueries = new ArrayList<>();
        for (int i = 0; i < queries.length; i++) {
            String[] splitQuery = queries[i].split("=");
            int j = 0;
            while (j < sortedQueries.size()) {
                String[] splitCurrent = sortedQueries.get(j).split("=");
                if (splitQuery[0].compareTo(splitCurrent[0]) <= 0) {
                    break;
                }
                j++;
            }
            sortedQueries.add(j, queries[i]);
            
        }
        return sortedQueries;
    }

    /*
     * @url -> full url being formatted
     * 
     * parses @url for query and formats it for proper output
     * 
     * returns the formatted url, does not modify strings without queries
     */
    public static String formatUrl(String url) {
        try {
            int queryIndex = url.indexOf("?");
            if (queryIndex == -1) {
                return url;
            }
            String formatUrl = url.substring(0, url.indexOf("?"));
            String queryString = url.substring(url.indexOf("?") + 1);
            String[] queries = queryString.split("&");
            ArrayList<String> sortedQueries = buildQueryList(queries);

            for (String query : sortedQueries) {
                formatUrl += "\n\t" + query;
            }
            return formatUrl;
        } catch (PatternSyntaxException error) {
            return url;
        }
    }

    /*
     * @current -> current Element node of the level-order traversal
     * @urlList -> list of valid urls discovered through a level-order
     *               traversal of the html doc
     * @rootUrl -> root url of the html file being parsed
     * 
     * returns true if the url is in an element with an <a> tag, a reachable
     *  link, and the link is not a duplicate
     *  returns false if the link is not valid
     */
    public static boolean isValidUrl(Element current, String[] urlList, String rootUrl) {
        if (!current.tagName().equals("a")) {
            return false;
        }

        String url = current.absUrl("href");
        if (url.equals("")) {
            return false;
        }
        if (contains(urlList, url)) {
            return false;
        }

        String relUrl = current.attributes().get("href");
        if (relUrl.charAt(0) == '#') {
            return false;
        }

        return true;
    }

    /*
     * @current -> current Element node of the level-order traversal
     * @siblingQueue -> a Priority Queue holding visited element nodes
     * 
     * returns the next element node in a level-order traversal of the html doc
     *  returns null if the end of the document has been reached
     */
    public static Element getNextElement(Element current, ArrayList<Element> siblingQueue) {
        current = current.nextElementSibling();

        while (current == null && !siblingQueue.isEmpty()) {
            current = siblingQueue.remove(0).firstElementChild();
        }

        if (current != null) {
            siblingQueue.add(current);
        }

        return current;
    }

    /*
     * @doc -> Jsoup Document object returned from Jsoup Connection object
     * @rootUrl -> root url of the html file being parsed
     * @maxUrls -> the maximum number of urls that can be output
     * 
     * returns a list of valid urls discovered in a level-order
     *  traversel of @doc
     */
    public static String[] getUrlList(Document doc, String rootUrl, int maxUrls) {
        ArrayList<Element> siblingQueue = new ArrayList<Element>();
        String[] urlList = new String[maxUrls];
        Element current = doc.firstElementChild();
        int urlCount = 0;
        siblingQueue.add(current);

        while (current != null && urlCount < maxUrls) {
            if (isValidUrl(current, urlList, rootUrl)) {
                String url = current.absUrl("href");
                urlList[urlCount] = formatUrl(url);
                urlCount++;
            }
            
            current = getNextElement(current, siblingQueue);
        }

        return urlList;
    }

    /*
     * @rootUrl -> root url of the html file being parsed
     * @maxUrls -> the maximum number of urls that can be output
     * @timeout -> timeout for the html file parsing (in milliseconds)
     * 
     * prints a spiderweb, a list of links that are reachable
     *  from @rootUrl
     * 
     * prints links that have been discovered if a timeout occurs
     *  before the html file can be fully parsed
     */
    public static void spiderWeb(String rootUrl, int maxUrls, int timeout) {
        String[] urlList = {};
        try {
            Document doc = Jsoup.connect(rootUrl).timeout(1500 + timeout).followRedirects(true).get();
            urlList = getUrlList(doc, rootUrl, maxUrls);
        } catch (SocketTimeoutException timeoutError) {
        } catch (IOException ioError) {
            ioError.printStackTrace();
            return;
        } finally {
            print(urlList);
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("invalid number of args input");
            return;
        }

        String rootUrl = args[0];
        String scheme = rootUrl.substring(0, rootUrl.indexOf("://"));
        if (!scheme.equals("http") && !scheme.equals("https")) {
            System.out.println("invalid scheme in root url");
            return;
        }

        int maxUrls = Integer.parseInt(args[1]);
        if (maxUrls < 1 || maxUrls > 10000) {
            System.out.println("invalid maximum number of reachable urls, must be 1 - 10000");
            return;
        }

        int timeout = Integer.parseInt(args[2]);
        if (timeout < 1 || timeout > 10000) {
            System.out.println("invalid timeout value, must be 1 - 10000");
            return;
        }

        spiderWeb(rootUrl, maxUrls, timeout);
    }
}