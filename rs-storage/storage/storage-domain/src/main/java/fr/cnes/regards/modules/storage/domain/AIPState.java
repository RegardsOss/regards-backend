/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

/**
 * Represent the state of an AIP.
 * State transition from top to bottom unless indicated otherwise.
 * 
 * <pre>
 *              VALID
 *                |
 *             PENDING
 *             /     \
 *            /       \
 *           /         \
 *          /           \
 * STORAGE_ERROR -> STORING_METADATA
 *        |                 |     |^
 *        |                 |     |
 *        |                 |  UPDATED
 *        |                 |  /^
 *        |              STORED
 *        |              /
 *        |             /
 *        |            /
 *        |           /
 *        |          /
 *        |         /
 *        |        /
 *        |       /
 *        |      /
 *        DELETED
 * </pre>
 * 
 * @author Sylvain Vissiere-Guerinet
 *
 */
public enum AIPState implements IAipState {
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
     * Data and metadata storages have ended successfully
     */
    STORED,
    /**
     * Data or metadata storage has encountered a problem
     */
    STORAGE_ERROR,
    UPDATED,
    /**
     * AIP has been logically deleted
     */
    DELETED;

    @Override
    public String getName() {
        return this.name();
    }
}
