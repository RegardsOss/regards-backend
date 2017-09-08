/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain;

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
     * STAF store error.
     */
    ERROR;

}
