/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.domain;

/**
 * Define specific constant query terms
 * @author Xavier-Alexandre Brochard
 */
public enum Terms {

    GROUPS("groups");

    /**
     * The textual term
     */
    private final String name;

    /**
     * @param pName
     */
    private Terms(String pName) {
        name = pName;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

}
