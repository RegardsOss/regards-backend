/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class EntityCorruptByNetworkException extends EntityException {

    /**
     * @param pMessage
     */
    public EntityCorruptByNetworkException(String pMessage) {
        super(pMessage);
    }

    /**
     * @param pIpId
     * @param pCalculatedChecksum
     */
    public EntityCorruptByNetworkException(String pIpId, String pCalculatedChecksum, String pChecksum) {
        this("Entity " + pIpId + " has been corrupted during the transport expected checksum: " + pChecksum
                + " calculated checksum: " + pCalculatedChecksum);
    }

}
