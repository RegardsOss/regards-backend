/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.FeignClientTests.Hello;
import fr.cnes.regards.framework.feign.annotation.RestClient;

/**
 * TODO
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "localapp", url = "http://localhost:30333")
public interface IHelloClient {

    @RequestMapping(method = RequestMethod.GET, value = "/hello")
    ResponseEntity<Hello> getHello();

    @RequestMapping(method = RequestMethod.GET, value = "/hello404")
    ResponseEntity<Hello> getHello404();

    @RequestMapping(method = RequestMethod.GET, value = "/hello503")
    ResponseEntity<Hello> getHello503();
}
