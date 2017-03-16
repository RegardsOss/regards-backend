/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;

/**
 * List the acceptable search types for the {@link CatalogController} endpoints.
 *
 * @author Xavier-Alexandre Brochard
 */
public enum ResultClass {
    COLLECTION("Collection", Collection.class), DATASET("Dataset", Dataset.class), DATAOBJECT("DataObject",
            DataObject.class), DOCUMENT("Document", Document.class);

    private final String name;

    private final Class<?> clazz;

    ResultClass(String pName, Class<?> pClazz) {
        name = pName;
        clazz = pClazz;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the clazz
     */
    public Class<?> getClazz() {
        return clazz;
    }

}
