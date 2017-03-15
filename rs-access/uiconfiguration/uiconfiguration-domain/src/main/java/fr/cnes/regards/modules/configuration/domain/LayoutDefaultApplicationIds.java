/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.domain;

/**
 *
 * Class LayoutDefaultApplicationIds
 *
 * Enumeration for know IHM applications that needs to configure a layout.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public enum LayoutDefaultApplicationIds {
    USER("user"), PORTAL("portal");

    private String value;

    private LayoutDefaultApplicationIds(final String pValue) {
        value = pValue;
    }

    @Override
    public String toString() {
        return this.value;
    }

}
