/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.${artifactId}.client;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;

import fr.cnes.regards.modules.${artifactId}.domain.Greeting;

public class GreetingsClientFallback implements IGreetingsClient {

    private static final Logger LOG = LoggerFactory.getLogger(GreetingsClientFallback.class);

    @Override
    public Greeting greeting(String pName) {
        LOG.error("Error greeting");
        return new Greeting();
    }
    
    public Greeting me(String pName) {
        LOG.error("Error me");
        return new Greeting();
    }

}