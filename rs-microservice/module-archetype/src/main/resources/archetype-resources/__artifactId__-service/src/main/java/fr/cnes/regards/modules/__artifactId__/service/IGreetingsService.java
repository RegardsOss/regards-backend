package fr.cnes.regards.modules.${artifactId}.service;

import fr.cnes.regards.modules.${artifactId}.domain.Greeting;

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
