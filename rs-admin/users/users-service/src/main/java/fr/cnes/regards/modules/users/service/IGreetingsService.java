package fr.cnes.regards.modules.users.service;

import fr.cnes.regards.modules.users.domain.Greeting;

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
