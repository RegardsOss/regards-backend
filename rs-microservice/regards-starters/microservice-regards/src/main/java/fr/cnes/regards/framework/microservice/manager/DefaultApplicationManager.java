/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.manager;

import java.io.IOException;

/**
 * @author svissier
 *
 */
public class DefaultApplicationManager implements IApplicationManager {

    @Override
    public void immediateShutdown() throws IOException {
        Runtime.getRuntime().exit(0); // NOSONAR
    }

}
