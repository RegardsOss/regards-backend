/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.client;

import fr.cnes.regards.client.core.ClientFactory;

/**
 * 
 * TODO Description
 * @author TODO
 *
 */
public interface IGreetingsClient {
    
    static IGreetingsClient getClient(String pUrl, String pJwtToken) {
        return ClientFactory.build(IGreetingsClient.class, new GreetingsClientFallback(), pUrl, pJwtToken);
    }

//    /**
//     * Rest resource /api/greeting/{name} Method GET
//     *
//     * @param name
//     * @return
//     */
//    @RequestLine("GET /api/greeting")
//    Greeting greeting(String pName);
//
//    /**
//     * Rest resource /api/me/{name} Method GET
//     *
//     * @param name
//     * @return
//     */
//    @RequestLine("GET /api/me")
//    Greeting me(String pName);

}
