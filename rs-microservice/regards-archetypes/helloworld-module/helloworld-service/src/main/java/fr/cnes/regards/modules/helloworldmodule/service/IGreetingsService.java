/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.helloworldmodule.service;

import fr.cnes.regards.modules.helloworldmodule.domain.Greeting;

/**
 * 
 * TODO Description
 * @author TODO
 *
 */
public interface IGreetingsService {

    /**
     *
     * Get greetings
     *
     * @return {@link Greeting}
     * @since 1.0.0
     */
    Greeting getGreeting(String pName);

}
