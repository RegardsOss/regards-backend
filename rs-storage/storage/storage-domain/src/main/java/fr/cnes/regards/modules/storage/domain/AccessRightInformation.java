package fr.cnes.regards.modules.storage.domain;

public class AccessRightInformation extends Information {

    public AccessRightInformation() {
        super();
    }

    public AccessRightInformation generate() {
        this.addMetadata(new KeyValuePair("READ", "Group1"));
        return this;
    }

}
