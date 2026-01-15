package se.skl.tp.vp.utils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;


public class FileUtils {
    /**
     * Reads the content of a classpath resource from a URL and returns it as a String.
     *
     * @param url The URL of the resource (e.g., from getClass().getClassLoader().getResource()).
     * @return The content of the resource as a String.
     * @throws IllegalStateException If the resource cannot be read.
     */
    public static String readFile(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null");
        }
        try (InputStream is = url.openStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + url, e);
        }
    }
}
