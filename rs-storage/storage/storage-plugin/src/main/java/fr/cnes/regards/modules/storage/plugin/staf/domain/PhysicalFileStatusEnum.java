package fr.cnes.regards.modules.storage.plugin.staf.domain;

public enum PhysicalFileStatusEnum {

    /**
     * File is not ready to store yet.
     */
    PENDING,

    /**
     * File can be store into the STAF archive.
     */
    TO_STORE,

    /**
     * File has been stoted into the STAF archive.
     */
    STORED,

    /**
     * Error during STAF archive.
     */
    ERROR;

}
