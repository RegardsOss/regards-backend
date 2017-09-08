/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Event(target = Target.ALL)
public class AIPEvent implements ISubscribable {

    private AIPState aipState;

    /**
     * IP ID of the AIP
     */
    private String ipId;


    public AIPEvent(AIP aip) {
        ipId = aip.getIpId();
        aipState = aip.getState();
    }

    public String getIpId() {
        return ipId;
    }

    public void setIpId(String pIpId) {
        ipId = pIpId;
    }

    public AIPState getAipState() {
        return aipState;
    }

    public void setAipState(AIPState aipState) {
        this.aipState = aipState;
    }
}
