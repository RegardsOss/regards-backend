/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa;

import javax.persistence.MappedSuperclass;

/**
 * @param <ID>
 * @author msordi
 *
 */
@MappedSuperclass
public class Entity<ID> {

    /**
     * Entity identifier
     */
    private ID id;

    public ID getId() {
        return id;
    }

    public void setId(ID pId) {
        id = pId;
    }

}
