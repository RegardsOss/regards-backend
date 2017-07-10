/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.cnes.regards.framework.feign.annotation.RestClient;

/**
 *
 * TODO Description
 *
 * @author TODO
 *
 */
@RestClient(name = "rs-storage")
@RequestMapping(value = "/api", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IGreetingsClient {

    // /**
    // * Rest resource /api/greeting/{name} Method GET
    // *
    // * @param name
    // * @return
    // */
    // @RequestMapping(method = RequestMethod.GET, value = "/greeting")
    // @ResponseBody
    // public HttpEntity<Resource<Greeting>> greeting(String pName);
    //
    // /**
    // * Rest resource /api/me/{name} Method GET
    // *
    // * @param name
    // * @return
    // */
    // @RequestMapping(method = RequestMethod.GET, value = "/me")
    // @ResponseBody
    // public HttpEntity<Resource<Greeting>> me(String pName);

}
