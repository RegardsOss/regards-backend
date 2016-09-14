/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.Greeting;

@Service
public class GreetingsService implements IGreetingsService {

    @Override
    public Greeting getGreeting(String pName) {
        // return new GreetingAction(pName).execute();
        return null;
    }
}