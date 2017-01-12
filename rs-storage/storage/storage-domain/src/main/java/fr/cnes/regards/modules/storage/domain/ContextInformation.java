package fr.cnes.regards.modules.storage.domain;

public class ContextInformation extends Information {

    public ContextInformation() {
        super();
    }

    public ContextInformation generate() {
        this.addMetadata(new KeyValuePair("context", "OK"));
        return this;
    }

}
