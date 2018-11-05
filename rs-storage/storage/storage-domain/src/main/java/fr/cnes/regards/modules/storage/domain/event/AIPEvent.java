/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * Biggest granularity information event on what's happening on an AIP. If you need informations on each
 * StorageDataFile,
 * {@link DataFileEvent}.
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class AIPEvent implements ISubscribable {

    /**
     * Failure cause message format
     */
    private static final String FAILURE_CAUSE_TEMPLATE = "File %s could not be stored by IDataStorage( plugin "
            + "configuration id: %s)";

    /**
     * The aip state
     */
    private AIPState aipState;

    /**
     * IP ID of the AIP
     */
    private String aipId;

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
    @SuppressWarnings("unused")
    private AIPEvent() {
        // Nothing to do
    }

    /**
     * Constructor initializing the event from an aip
     * @param aip
     */
    public AIPEvent(AIP aip) {
        aipId = aip.getId().toString();
        aipState = aip.getState();
        sipId = aip.getSipId().isPresent() ? aip.getSipId().get().toString() : null;
    }

    /**
     * Constructor initializing the event from an aip and uses a data file url and a plugin configuration id to make the
     * failure cause message
     * @param aip
     * @param dataFileUrl
     * @param pluginConfId
     */
    public AIPEvent(AIP aip, String dataFileUrl, Long pluginConfId) {
        aipId = aip.getId().toString();
        aipState = aip.getState();
        sipId = aip.getSipId().isPresent() ? aip.getSipId().get().toString() : null;
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
    public String getAipId() {
        return aipId;
    }

    /**
     * Set the ip id
     * @param pIpId
     */
    public void setIpId(String pIpId) {
        aipId = pIpId;
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
