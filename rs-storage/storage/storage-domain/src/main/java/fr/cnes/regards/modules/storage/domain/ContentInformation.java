package fr.cnes.regards.modules.storage.domain;

public class ContentInformation {

    private DataObject dataObject;

    private RepresentationInformation representationInformation;

    public ContentInformation() {
    }

    public DataObject getDataObject() {
        return dataObject;
    }

    public void setDataObject(DataObject pDataObject) {
        dataObject = pDataObject;
    }

    public RepresentationInformation getRepresentationInformation() {
        return representationInformation;
    }

    public void setRepresentationInformation(RepresentationInformation pRepresentationInformation) {
        representationInformation = pRepresentationInformation;
    }

    public ContentInformation generate() {

        this.dataObject = new DataObject().generate();
        this.representationInformation = new RepresentationInformation().generate();
        return this;
    }

}
