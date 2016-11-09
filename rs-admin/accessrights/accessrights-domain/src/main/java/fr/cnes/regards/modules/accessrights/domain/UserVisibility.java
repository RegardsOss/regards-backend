/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain;

/*
 * LICENSE_PLACEHOLDER
 */
public enum UserVisibility {
    /**
     * Associated meta data is only readable by the user
     */
    READABLE,
    /**
     * Associated meta data is writable by the user
     */
    WRITEABLE,
    /**
     * Associated meta data is hidden from the user
     */
    HIDDEN;

    @Override
    public String toString() {
        return name();
    }
}
