/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.manage;

import java.io.IOException;

import org.springframework.context.ApplicationContext;

/**
 * @author svissier
 *
 */
public class ApplicationManager {

    private final ApplicationContext applicationContext;

    public ApplicationManager(ApplicationContext pApplicationContext) {
        applicationContext = pApplicationContext;
    }

    public void immediateShutdown() throws IOException {
        Runtime.getRuntime().exit(0); // NOSONAR
    }

}
