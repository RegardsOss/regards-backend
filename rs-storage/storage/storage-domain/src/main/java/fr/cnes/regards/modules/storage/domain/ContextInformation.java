/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

public class ContextInformation extends Information {

    public ContextInformation() {
        super();
    }

    public ContextInformation generate() {
        addMetadata("context", "OK");
        return this;
    }

}
