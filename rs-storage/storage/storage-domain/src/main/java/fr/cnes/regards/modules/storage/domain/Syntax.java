/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class Syntax implements Serializable {

    @NotNull
    private String description;

    @NotNull
    private String mimeType;

    @NotNull
    private String name;

    public Syntax() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String pMimeType) {
        mimeType = pMimeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public Syntax generate() {
        description = "SYNTAX_DESCRIPTION";
        mimeType = "application/name";
        name = "NAME";
        return this;
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof Syntax) && description.equals(((Syntax) pOther).description)
                && name.equals(((Syntax) pOther).name) && mimeType.equals(((Syntax) pOther).mimeType);
    }

    @Override
    public int hashCode() {
        return description.hashCode();
    }

}
