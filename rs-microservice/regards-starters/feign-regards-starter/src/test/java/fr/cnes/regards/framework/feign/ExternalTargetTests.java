/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.feign;


import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;

/**
 * @author rmechali
 */
@RunWith(SpringRunner.class)
public class ExternalTargetTests {


    /**
     * Asserts header is present and has expected value in header map
     * @param headers headers map
     * @param headerName name of the header to test
     * @param expectedHeaderValue expected header value
     */
    private void assertHeaderIn(Map<String, Collection<String>> headers, String headerName, String expectedHeaderValue){
        Collection<String> headerValue = headers.get(headerName);
        Assert.assertNotNull("There should be " + headerName + " header", headerValue);
        Assert.assertEquals("Header" + headerName + " should be one element length", 1, headerValue.size());
        Assert.assertEquals("Header" + headerName + " value should be correctly set", expectedHeaderValue, headerValue.iterator().next());
    }


    /**
     * Check added default headers
     */
    @Test
    public void testStandardHeaders() {
        //noinspection unchecked
        ExternalTarget<String> target = new ExternalTarget<>(String.class, "http://my.domain.com:5156/myFolder?name=46&views=35");
        Map<String, Collection<String>> headers = target.getHeaders();
        assertHeaderIn(headers, HttpHeaders.USER_AGENT, "regards");
        assertHeaderIn(headers, HttpHeaders.HOST, "my.domain.com:5156");

        //noinspection unchecked
        target = new ExternalTarget<>(String.class, "http://www.elsewhere.com?q=nothing");
        headers = target.getHeaders();
        assertHeaderIn(headers, HttpHeaders.USER_AGENT, "regards");
        assertHeaderIn(headers, HttpHeaders.HOST, "www.elsewhere.com");
    }

    @Test
    public void testUserHeaders() {
        // No standard header overriding
        ExternalTarget<String> target = new ExternalTarget<>(String.class, "http://my.domain.com:5157/myFolder?name=46&views=35", new AbstractMap.SimpleEntry<>(HttpHeaders.ACCEPT, "text/html"),
                new AbstractMap.SimpleEntry<>("Custom", "Something"));
        Map<String, Collection<String>> headers = target.getHeaders();
        assertHeaderIn(headers, HttpHeaders.USER_AGENT, "regards");
        assertHeaderIn(headers, HttpHeaders.HOST, "my.domain.com:5157");
        assertHeaderIn(headers, HttpHeaders.ACCEPT, "text/html");
        assertHeaderIn(headers, "Custom", "Something");

        // with standard headers overriding
        target = new ExternalTarget<>(String.class, "http://my.domain.com:5157/myFolder?name=46&views=35", new AbstractMap.SimpleEntry<>(HttpHeaders.ACCEPT, "text/html"),
                new AbstractMap.SimpleEntry<>("Custom", "Something"), new AbstractMap.SimpleEntry<>(HttpHeaders.USER_AGENT, "COUCOUCMOI"), new AbstractMap.SimpleEntry<>(HttpHeaders.HOST, "CCHEZVOUS"));
        headers = target.getHeaders();
        assertHeaderIn(headers, HttpHeaders.USER_AGENT, "COUCOUCMOI");
        assertHeaderIn(headers, HttpHeaders.HOST, "CCHEZVOUS");
        assertHeaderIn(headers, HttpHeaders.ACCEPT, "text/html");
        assertHeaderIn(headers, "Custom", "Something");





    }


}

