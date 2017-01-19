/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

public class AccessRightInformation extends Information {

    public AccessRightInformation() {
        super();
    }

    public AccessRightInformation generate() {
        addMetadata("READ", "Group1");
        return this;
    }

}
