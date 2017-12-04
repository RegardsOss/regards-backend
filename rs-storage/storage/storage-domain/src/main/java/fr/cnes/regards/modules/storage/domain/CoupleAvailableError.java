package fr.cnes.regards.modules.storage.domain;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * POJO allowing us to know which data files are in the cache and which data files could not be set into the cache
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class CoupleAvailableError {

    /**
     * Data files that are already in cache
     */
    private Set<DataFile> availables;

    /**
     * Data files that could not be set in cache
     */
    private Set<DataFile> errors;

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
    public CoupleAvailableError(Set<DataFile> availables, Set<DataFile> errors) {
        this.availables = availables;
        this.errors = errors;
    }

    /**
     * @return the availables
     */
    public Set<DataFile> getAvailables() {
        return availables;
    }

    /**
     * Set the availables
     * @param availables
     */
    public void setAvailables(Set<DataFile> availables) {
        this.availables = availables;
    }

    /**
     * @return the errors
     */
    public Set<DataFile> getErrors() {
        return errors;
    }

    /**
     * Set the errors
     * @param errors
     */
    public void setErrors(Set<DataFile> errors) {
        this.errors = errors;
    }
}
