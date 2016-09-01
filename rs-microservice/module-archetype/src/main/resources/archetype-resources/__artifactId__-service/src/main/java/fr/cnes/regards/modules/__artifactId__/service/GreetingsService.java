package fr.cnes.regards.modules.${artifactId}.service;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.${artifactId}.domain.Greeting;
import fr.cnes.regards.modules.${artifactId}.service.actions.GreetingAction;

@Service
public class GreetingsService implements IGreetingsService {

    @Override
    public Greeting getGreeting(String pName) {
        return new GreetingAction(pName).execute();
    }

}