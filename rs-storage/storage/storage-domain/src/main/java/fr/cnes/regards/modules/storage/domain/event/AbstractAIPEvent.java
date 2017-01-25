/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.event;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import fr.cnes.regards.framework.amqp.event.EventProperties;
import fr.cnes.regards.framework.amqp.event.ISubscribableEvent;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@EventProperties(target = Target.ALL, mode = WorkerMode.ALL)
public abstract class AbstractAIPEvent implements ISubscribableEvent {

    /**
     * IP ID of the AIP
     */
    private String ipId;

    /**
     * checksum of the AIP
     */
    private String checksum;

    public AbstractAIPEvent(AIP pAIP) throws NoSuchAlgorithmException, IOException {
        ipId = pAIP.getIpId();
        checksum = pAIP.getChecksum();
    }

    public String getIpId() {
        return ipId;
    }

    public void setIpId(String pIpId) {
        ipId = pIpId;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String pChecksum) {
        checksum = pChecksum;
    }

}
