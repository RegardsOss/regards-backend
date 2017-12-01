/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * Biggest granularity information event on what's happening on an AIP. If you need informations on each DataFile,
 * {@link DataFileEvent}.
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Event
public class AIPEvent implements ISubscribable {

    private static final String FAILURE_CAUSE_TEMPLATE = "File %s could not be stored by IDataStorage( plugin configuration id: %s)";

    /**
     * The aip state
     */
    private AIPState aipState;

    /**
     * IP ID of the AIP
     */
    private String ipId;

    /**
     * The failure cause
     */
    private String failureCause;

    /**
     * The aip sip id
     */
    private String sipId;

    /**
     * Default constructor
     */
    private AIPEvent() {
    }

    /**
     * Constructor initializing the event from an aip
     * @param aip
     */
    public AIPEvent(AIP aip) {
        ipId = aip.getId().toString();
        aipState = aip.getState();
        sipId = aip.getSipId();
    }

    /**
     * Constructor initializing the event from an aip and uses a data file url and a plugin configuration id to make the failure cause message
     * @param aip
     * @param dataFileUrl
     * @param pluginConfId
     */
    public AIPEvent(AIP aip, String dataFileUrl, Long pluginConfId) {
        ipId = aip.getId().toString();
        aipState = aip.getState();
        sipId = aip.getSipId();
        failureCause = String.format(FAILURE_CAUSE_TEMPLATE, dataFileUrl, pluginConfId);
    }

    /**
     * @return the sip id
     */
    public String getSipId() {
        return sipId;
    }

    /**
     * Set the sip id
     * @param sipId
     */
    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    /**
     * @return the ip id
     */
    public String getIpId() {
        return ipId;
    }

    /**
     * Set the ip id
     * @param pIpId
     */
    public void setIpId(String pIpId) {
        ipId = pIpId;
    }

    /**
     * @return the aip state
     */
    public AIPState getAipState() {
        return aipState;
    }

    /**
     * Set the aip state
     * @param aipState
     */
    public void setAipState(AIPState aipState) {
        this.aipState = aipState;
    }

    /**
     * @return the failure cause
     */
    public String getFailureCause() {
        return failureCause;
    }

    /**
     * Set the failure cause
     * @param failureCause
     */
    public void setFailureCause(String failureCause) {
        this.failureCause = failureCause;
    }
}
