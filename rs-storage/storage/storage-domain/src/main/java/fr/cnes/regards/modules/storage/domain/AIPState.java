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
     * Store Job has been scheduled
     */
    PENDING,
    /**
     * Store Job has finish successfully
     */
    STORED,
    /**
     * Store Job has encountered a problem
     */
    STORAGE_ERROR,
    /**
     * AIP has been logically deleted
     */
    DELETED;
}
