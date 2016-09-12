/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.${moduleName}.signature;

import feign.RequestLine;

import fr.cnes.regards.modules.${moduleName}.domain.Greeting;

/**
 * 
 * TODO Description
 * @author TODO
 *
 */
public interface IGreetingsSignature {

    /**
     * Rest resource /api/greeting/{name} Method GET
     *
     * @param name
     * @return
     */
    @RequestLine("GET /api/greeting")
    Greeting greeting(String pName);

    /**
     * Rest resource /api/me/{name} Method GET
     *
     * @param name
     * @return
     */
    @RequestLine("GET /api/me")
    Greeting me(String pName);

}
