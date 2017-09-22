package fr.cnes.regards.modules.storage.domain.database;

import java.util.Set;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class CoupleAvailableError {

    private Set<DataFile> availables;

    private Set<DataFile> errors;

    protected CoupleAvailableError() {
    }

    public CoupleAvailableError(Set<DataFile> availables, Set<DataFile> errors) {
        this.availables = availables;
        this.errors = errors;
    }

    public Set<DataFile> getAvailables() {
        return availables;
    }

    public void setAvailables(Set<DataFile> availables) {
        this.availables = availables;
    }

    public Set<DataFile> getErrors() {
        return errors;
    }

    public void setErrors(Set<DataFile> errors) {
        this.errors = errors;
    }
}
