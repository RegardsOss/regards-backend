/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

/**
 * Represent the state of an AIP.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public enum AIPState {
    /**
     * AIP has been validated, network has not corrupted it
     */
    VALID,
    /**
     * Data storage has been scheduled
     */
    PENDING,
    /**
     * Metadata storage has been scheduled
     */
    STORING_METADATA,
    /**
     * Data and metadata storage has finish successfully
     */
    STORED,
    /**
     * Data or metadata storage has encountered a problem
     */
    STORAGE_ERROR,
    /**
     * AIP has been logically deleted
     */
    DELETED
}
