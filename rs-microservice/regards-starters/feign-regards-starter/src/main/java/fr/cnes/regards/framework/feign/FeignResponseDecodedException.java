package fr.cnes.regards.framework.feign;

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Exception used by {@link ClientErrorDecoder} to allow us to get access to a feign client response body when the result status is not one of the success class.
 * @author Sylvain VISSIERE-GUERINET
 */
@SuppressWarnings("serial")
public class FeignResponseDecodedException extends HttpStatusCodeException {

    private final Object body;

    protected FeignResponseDecodedException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders,
            byte[] responseBody, Charset responseCharset, Object body) {
        super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
        this.body = body;
    }

    public Object getBody() {
        return body;
    }
}
