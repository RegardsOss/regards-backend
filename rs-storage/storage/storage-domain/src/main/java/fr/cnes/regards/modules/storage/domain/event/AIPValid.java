/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.event;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class AIPValid extends AbstractAIPEvent {

    public AIPValid(AIP pAIP) throws NoSuchAlgorithmException, IOException {
        super(pAIP);
    }

}
