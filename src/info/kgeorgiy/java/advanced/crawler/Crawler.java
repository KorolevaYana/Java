package info.kgeorgiy.java.advanced.crawler;

import java.io.IOException;
import java.util.List;

/**
 * Crawls web sites.
 *
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public interface Crawler extends AutoCloseable {
    List<String> download(String url, int depth) throws IOException;

    void close();
}
