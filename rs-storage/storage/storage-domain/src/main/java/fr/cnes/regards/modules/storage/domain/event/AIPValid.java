/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class AIPValid extends AbstractAIPEvent {

    public AIPValid(AIP pAIP) {
        super(pAIP);
    }

}
