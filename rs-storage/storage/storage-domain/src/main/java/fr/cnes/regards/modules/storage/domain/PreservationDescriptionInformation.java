package fr.cnes.regards.modules.storage.domain;

import java.security.NoSuchAlgorithmException;

public class PreservationDescriptionInformation {

    private AccessRightInformation accesRightInformation;

    private ContextInformation contextInformation;

    private FixityInformation fixityInformation;

    private ProvenanceInformation provenanceInformation;

    public PreservationDescriptionInformation() {

    }

    public AccessRightInformation getAccesRightInformation() {
        return accesRightInformation;
    }

    public void setAccesRightInformation(AccessRightInformation pAccesRightInformation) {
        accesRightInformation = pAccesRightInformation;
    }

    public ContextInformation getContextInformation() {
        return contextInformation;
    }

    public void setContextInformation(ContextInformation pContextInformation) {
        contextInformation = pContextInformation;
    }

    public FixityInformation getFixityInformation() {
        return fixityInformation;
    }

    public void setFixityInformation(FixityInformation pFixityInformation) {
        fixityInformation = pFixityInformation;
    }

    public ProvenanceInformation getProvenanceInformation() {
        return provenanceInformation;
    }

    public void setProvenanceInformation(ProvenanceInformation pProvenanceInformation) {
        provenanceInformation = pProvenanceInformation;
    }

    public PreservationDescriptionInformation generate() throws NoSuchAlgorithmException {
        this.accesRightInformation = new AccessRightInformation().generate();
        this.contextInformation = new ContextInformation().generate();
        this.fixityInformation = new FixityInformation().generate();
        this.provenanceInformation = new ProvenanceInformation().generate();
        return this;
    }

}
