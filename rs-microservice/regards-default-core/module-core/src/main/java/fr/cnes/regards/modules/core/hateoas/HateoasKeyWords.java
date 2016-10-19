/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.hateoas;

/**
 *
 * Class HateoasKeyWords
 *
 * Enumeration for Hateoas common key words
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public enum HateoasKeyWords {

    /**
     * CREATE entity keyword
     */
    CREATE("create"),

    /**
     * UPDATE entity keyword
     */
    UPDATE("update"),

    /**
     * DELETE entity keyword
     */
    DELETE("delete"),
    /**
     * SELF entity keyword
     */
    SELF("self");

    /**
     * Value
     */
    private String value = "";

    private HateoasKeyWords(final String pValue) {
        value = pValue;
    }

    public String getValue() {
        return value;
    }

}
