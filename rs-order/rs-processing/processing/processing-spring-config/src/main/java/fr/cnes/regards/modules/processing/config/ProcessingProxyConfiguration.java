package fr.cnes.regards.modules.processing.config;

import com.google.common.base.Strings;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.stream.Stream;

@Configuration
public class ProcessingProxyConfiguration {

    @Bean public Proxy proxy(HttpProxyProperties config) {
        Proxy proxy = Strings.isNullOrEmpty(config.getHost())
                ? Proxy.NO_PROXY
                : new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getHost(), Option.of(config.getPort()).getOrElse(80)));
        return proxy;
    }

    @Bean("nonProxyHosts") public Set<String> nonProxyHosts(HttpProxyProperties config) {
        return Stream
            .of(Option.of(config.getNoproxy())
                .getOrElse(() -> "")
                .split(",")
            )
            .filter(s -> !s.isEmpty())
            .map(String::trim)
            .collect(HashSet.collector());
    }
}

