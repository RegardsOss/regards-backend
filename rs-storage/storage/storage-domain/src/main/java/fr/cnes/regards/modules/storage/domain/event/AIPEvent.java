/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * Biggest granularity information event on what's happening on an AIP. If you need ionformations on each DataFile,
 * {@link DataFileEvent}.
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Event
public class AIPEvent implements ISubscribable {

    private static final String FAILURE_CAUSE_TEMPLATE = "File %s could not be stored by IDataStorage( plugin configuration id: %s)";

    private AIPState aipState;

    /**
     * IP ID of the AIP
     */
    private String ipId;

    private String failureCause;

    private String sipId;

    private AIPEvent() {
    }

    public AIPEvent(AIP aip) {
        ipId = aip.getId().toString();
        aipState = aip.getState();
        sipId = aip.getSipId();
    }

    public AIPEvent(AIP aip, String dataFileUrl, Long pluginConfId) {
        ipId = aip.getId().toString();
        aipState = aip.getState();
        sipId = aip.getSipId();
        failureCause = String.format(FAILURE_CAUSE_TEMPLATE, dataFileUrl, pluginConfId);
    }

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
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

    public String getFailureCause() {
        return failureCause;
    }

    public void setFailureCause(String failureCause) {
        this.failureCause = failureCause;
    }
}
