/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.domain;

/**
 * Eumeration of all possible status for an {@link AbstractPhysicalFile}
 * @author Sébastien Binda
 *
 */
public enum PhysicalFileStatusEnum {

    /**
     * File is not ready get stored into the STAF Archive yet.
     */
    PENDING,

    /**
     * File is ready to get stored into the STAF Archive.
     */
    TO_STORE,

    /**
     * File has been stoted into the STAF archive.
     */
    STORED,

    /**
     * File has been stored in local temporary file system. (Waiting to be send to STAF System).
     */
    LOCALY_STORED,

    /**
     * File has to be retrieve from STAF System.
     */
    TO_RETRIEVE,

    /**
     * File has been successfully retrieved from STAF System.
     */
    RETRIEVED,

    /**
     * File is ready to be deleted from STAF System.
     */
    TO_DELETE,

    /**
     * File has been deleted from STAF System.
     */
    DELETED,

    /**
     * STAF store error.
     */
    ERROR;

}
