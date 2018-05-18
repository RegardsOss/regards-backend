package fr.cnes.regards.modules.storage.domain;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * POJO allowing us to know which data files are in the cache and which data files could not be set into the cache
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class CoupleAvailableError {

    /**
     * Data files that are already in cache
     */
    private Set<StorageDataFile> availables;

    /**
     * Data files that could not be set in cache
     */
    private Set<StorageDataFile> errors;

    /**
     * Default constructor
     */
    protected CoupleAvailableError() {
    }

    /**
     * Constructor setting the parameters as attributes
     * @param availables
     * @param errors
     */
    public CoupleAvailableError(Set<StorageDataFile> availables, Set<StorageDataFile> errors) {
        this.availables = availables;
        this.errors = errors;
    }

    /**
     * @return the availables
     */
    public Set<StorageDataFile> getAvailables() {
        return availables;
    }

    /**
     * Set the availables
     * @param availables
     */
    public void setAvailables(Set<StorageDataFile> availables) {
        this.availables = availables;
    }

    /**
     * @return the errors
     */
    public Set<StorageDataFile> getErrors() {
        return errors;
    }

    /**
     * Set the errors
     * @param errors
     */
    public void setErrors(Set<StorageDataFile> errors) {
        this.errors = errors;
    }
}
