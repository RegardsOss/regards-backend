package fr.cnes.regards.modules.processing.filters;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Adding an 'application/json' Content-Type header for all response which don't have one already.
 */
@Component
public class ContentTypeHeaderWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange,
            WebFilterChain webFilterChain) {
        HttpHeaders headers = serverWebExchange.getResponse().getHeaders();
        if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
        return webFilterChain.filter(serverWebExchange);
    }

}
