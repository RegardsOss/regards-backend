/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.client.core;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;

/**
 *
 * Class DynamicAuthTokenTarget
 *
 * Feign target to add JWT Token into headers of each request sent by the FeignClient
 *
 * @author CS
 * @since 1.0-SNAPSHOT.
 */
public class DynamicAuthTokenTarget<T> implements Target<T> {

    private final String url_;

    private final String token_;

    private final Class<T> clazz_;

    public DynamicAuthTokenTarget(Class<T> clazz, String pUrl, String pToken) {
        url_ = pUrl;
        token_ = pToken;
        clazz_ = clazz;
    }

    @Override
    public Request apply(RequestTemplate input) {
        input.insert(0, url_);
        input.header("Authorization", "Bearer " + token_);
        return input.request();
    }

    @Override
    public String name() {
        return clazz_.getName();
    }

    @Override
    public Class<T> type() {
        return clazz_;
    }

    @Override
    public String url() {
        return url_;
    }
}