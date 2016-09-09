package fr.cnes.regards.modules.access.service;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.access.domain.Greeting;



@Service
public class GreetingsService implements IGreetingsService {

    @Override
    public Greeting getGreeting(String pName) {
        // return new GreetingAction(pName).execute();
        return null;
    }

}