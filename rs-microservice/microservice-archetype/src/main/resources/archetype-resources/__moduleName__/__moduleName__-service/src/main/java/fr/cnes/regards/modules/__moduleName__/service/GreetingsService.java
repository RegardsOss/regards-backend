package fr.cnes.regards.modules.${moduleName}.service;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.${moduleName}.domain.Greeting;
import fr.cnes.regards.modules.${moduleName}.service.actions.GreetingAction;

@Service
public class GreetingsService implements IGreetingsService {

    @Override
    public Greeting getGreeting(String pName) {
    	// return new GreetingAction(pName).execute();
        return null;
    }

}