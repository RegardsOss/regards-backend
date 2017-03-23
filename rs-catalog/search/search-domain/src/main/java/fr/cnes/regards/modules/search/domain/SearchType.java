/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.domain;

/**
 * List the acceptable search types for the {@link CatalogController} endpoints.
 *
 * @author Xavier-Alexandre Brochard
 */
public enum SearchType {
    ALL("all"), COLLECTION("collection"), DATASET("dataset"), DATA("dataobject"), DOCUMENT("document");

    private final String name;

    /**
     * @param pName
     */
    private SearchType(String pName) {
        name = pName;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

}