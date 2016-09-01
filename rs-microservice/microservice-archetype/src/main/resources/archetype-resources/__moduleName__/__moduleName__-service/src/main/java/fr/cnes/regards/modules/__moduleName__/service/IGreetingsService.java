package fr.cnes.regards.modules.${moduleName}.service;

import fr.cnes.regards.modules.${moduleName}.domain.Greeting;

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
