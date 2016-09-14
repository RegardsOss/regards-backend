/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import fr.cnes.regards.modules.accessRights.domain.Greeting;

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
