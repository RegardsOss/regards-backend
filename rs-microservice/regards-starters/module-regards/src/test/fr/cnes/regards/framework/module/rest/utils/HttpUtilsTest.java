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
    public void testRetrievePublicURIWithMultipleForwardedPort() throws MalformedURLException, URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.X_FORWARDED_PORT, "443,8080,9936");

        URI uri = HttpUtils.retrievePublicURI(request, "/some/endpoint", "aQueryParam");

        Assert.assertEquals(443, uri.getPort());
    }


    @Test
    public void testRetrievePublicURIWithOneForwardedPort() throws MalformedURLException, URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.X_FORWARDED_PORT, "443");

        URI uri = HttpUtils.retrievePublicURI(request, "/some/endpoint", "aQueryParam");

        Assert.assertEquals(443, uri.getPort());
    }
}