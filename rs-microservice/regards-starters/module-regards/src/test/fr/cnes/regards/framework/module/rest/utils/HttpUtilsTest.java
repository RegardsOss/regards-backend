package fr.cnes.regards.framework.module.rest.utils;

import com.google.common.net.HttpHeaders;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class HttpUtilsTest {

    @Test
    public void testRetrievePublicURIWithMultipleForward() throws MalformedURLException, URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.X_FORWARDED_PORT, "443,8080,9936");
        request.addHeader(HttpHeaders.X_FORWARDED_PROTO, "https,http");

        URI uri = HttpUtils.retrievePublicURI(request, "/some/endpoint", "aQueryParam");

        Assert.assertEquals(443, uri.getPort());
        Assert.assertEquals("https", uri.getScheme());
    }


    @Test
    public void testRetrievePublicURIWithOneForward() throws MalformedURLException, URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.X_FORWARDED_PORT, "443");
        request.addHeader(HttpHeaders.X_FORWARDED_PROTO, "https");

        URI uri = HttpUtils.retrievePublicURI(request, "/some/endpoint", "aQueryParam");

        Assert.assertEquals(443, uri.getPort());
        Assert.assertEquals("https", uri.getScheme());
    }
}