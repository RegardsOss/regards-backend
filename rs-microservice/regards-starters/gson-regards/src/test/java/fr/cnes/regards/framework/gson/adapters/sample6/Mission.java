/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample6;

import java.util.List;

/**
 * @author Marc Sordi
 *
 */
public class Mission {

    /**
     * name field
     */
    private String name;

    /**
     * field description
     */
    private String description;

    /**
     * Property list
     */
    private List<AbstractProperty<?>> properties;

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public List<AbstractProperty<?>> getProperties() {
        return properties;
    }

    public void setProperties(List<AbstractProperty<?>> pProperties) {
        properties = pProperties;
    }
}
