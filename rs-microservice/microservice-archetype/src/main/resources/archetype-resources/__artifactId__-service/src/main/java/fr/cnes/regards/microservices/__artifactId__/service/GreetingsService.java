package fr.cnes.regards.microservices.${artifactId}.service;

import org.springframework.stereotype.Service;

import fr.cnes.regards.microservices.${artifactId}.domain.Greeting;
import fr.cnes.regards.microservices.${artifactId}.service.actions.GreetingAction;

@Service
public class GreetingsService implements IGreetingsService {

    @Override
    public Greeting getGreeting(String pName) {
        return new GreetingAction(pName).execute();
    }

}