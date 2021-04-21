package fr.cnes.regards.framework.feign;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpHeaders;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;

/**
 * Target to access external API through URL. Provide an access to headers
 *
 * @param <T> Expected API results type, after conversion
 * @author rmechali
 */
public class ExternalTarget<T> implements Target<T> {

    /**
     * Feign client server url.
     */
    private final String url;

    /**
     * Feign client interface
     */
    private final Class<T> clazz;

    /**
     * Request headers
     */
    private final Map<String, Collection<String>> headers;

    /**
     * External target
     *
     * @param clazz   results class
     * @param url     target URL
     * @param headers user headers (higher priority than default ones)
     */
    public ExternalTarget(final Class<T> clazz, final String url, Map<String, String> headers) {
        this.url = url;
        this.clazz = clazz;
        this.headers = new HashMap<>();
        // 1 - Add common user agent header
        this.headers.put(HttpHeaders.USER_AGENT, Collections.singletonList("regards"));
        // 2 - Add common host header (host:port when port is specified)
        try {
            URL parsedURL = new URL(this.url);
            String host = parsedURL.getHost();
            int port = parsedURL.getPort();
            if (port != -1) {
                host += ":" + port;
            }
            this.headers.put(HttpHeaders.HOST, Collections.singletonList(host));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not instantiate external target as URL is invalid", e); // NOSONAR
        }
        // 3 - Add any user header (override default ones if there are specified)
        if (headers != null) {
            for (Entry<String, String> entry : headers.entrySet()) {
                this.headers.put(entry.getKey(), Collections.singletonList(entry.getValue()));
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Request apply(RequestTemplate pTemplate) {
        if (pTemplate.url().indexOf("http") != 0) {
            pTemplate.insert(0, url);
        }
        // Apply headers
        pTemplate.headers(this.headers);
        return pTemplate.request();
    }

    @Override
    public String name() {
        return "resources";
    }

    @Override
    public Class<T> type() {
        return clazz;
    }

    @Override
    public String url() {
        return url;
    }

    /**
     * @return headers
     */
    public Map<String, Collection<String>> getHeaders() {
        return headers;
    }
}
