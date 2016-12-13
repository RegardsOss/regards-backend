/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample7;

import java.util.List;

/**
 * @author Marc Sordi
 *
 */
public class Mission {

    private String name;

    private String description;

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
