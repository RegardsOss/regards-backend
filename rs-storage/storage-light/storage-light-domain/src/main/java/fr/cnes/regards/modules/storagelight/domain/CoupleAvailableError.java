package fr.cnes.regards.modules.storagelight.domain;

import java.util.Set;

import fr.cnes.regards.modules.storagelight.domain.database.FileReference;

/**
 * POJO allowing us to know which data files are in the cache and which data files could not be set into the cache
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class CoupleAvailableError {

    /**
     * Data files that are already in cache
     */
    private Set<FileReference> availables;

    /**
     * Data files that could not be set in cache
     */
    private Set<FileReference> errors;

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
    public CoupleAvailableError(Set<FileReference> availables, Set<FileReference> errors) {
        this.availables = availables;
        this.errors = errors;
    }

    /**
     * @return the availables
     */
    public Set<FileReference> getAvailables() {
        return availables;
    }

    /**
     * Set the availables
     * @param availables
     */
    public void setAvailables(Set<FileReference> availables) {
        this.availables = availables;
    }

    /**
     * @return the errors
     */
    public Set<FileReference> getErrors() {
        return errors;
    }

    /**
     * Set the errors
     * @param errors
     */
    public void setErrors(Set<FileReference> errors) {
        this.errors = errors;
    }
}
