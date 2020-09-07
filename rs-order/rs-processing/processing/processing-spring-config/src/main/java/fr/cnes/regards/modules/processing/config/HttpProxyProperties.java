package fr.cnes.regards.modules.processing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpProxyProperties {

    private final String host;
    private final Integer port;
    private final String noproxy;

    public HttpProxyProperties(
            @Value("${http.proxy.host:#{null}}") String host,
            @Value("${http.proxy.host:#{null}}") Integer port,
            @Value("${http.proxy.host:#{null}}") String noproxy
    ) {
        this.host = host;
        this.port = port;
        this.noproxy = noproxy;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getNoproxy() {
        return noproxy;
    }
}
