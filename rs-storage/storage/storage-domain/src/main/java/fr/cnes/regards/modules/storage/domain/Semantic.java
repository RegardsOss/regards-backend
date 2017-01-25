/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class Semantic implements Serializable {

    @NotNull
    private String description;

    public Semantic() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public Semantic generate() {
        description = "DESCRIPTION";
        return this;
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof Semantic) && description.equals(((Semantic) pOther).description);
    }

    @Override
    public int hashCode() {
        return description.hashCode();
    }

}
