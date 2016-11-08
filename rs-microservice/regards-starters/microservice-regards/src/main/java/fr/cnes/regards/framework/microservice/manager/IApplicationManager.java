/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.manager;

import java.io.IOException;

/**
 *
 * Microservice application manager
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 */
@FunctionalInterface
public interface IApplicationManager {

    void immediateShutdown() throws IOException;
}
