package fr.cnes.regards.modules.storage.domain;

import java.security.NoSuchAlgorithmException;

public class InformationObject {

    private ContentInformation contentInformation;

    private PreservationDescriptionInformation pdi;

    public InformationObject() {

    }

    public ContentInformation getContentInformation() {
        return contentInformation;
    }

    public void setContentInformation(ContentInformation pContentInformation) {
        contentInformation = pContentInformation;
    }

    public PreservationDescriptionInformation getPdi() {
        return pdi;
    }

    public void setPdi(PreservationDescriptionInformation pPdi) {
        pdi = pPdi;
    }

    public InformationObject generateRandomInformationObject() throws NoSuchAlgorithmException {
        this.contentInformation = new ContentInformation().generate();
        this.pdi = new PreservationDescriptionInformation().generate();
        return this;
    }

}
