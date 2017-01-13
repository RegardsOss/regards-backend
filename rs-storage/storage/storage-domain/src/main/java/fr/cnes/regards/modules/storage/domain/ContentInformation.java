/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.net.MalformedURLException;

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

    public ContentInformation generate() throws MalformedURLException {

        dataObject = new DataObject().generate();
        representationInformation = new RepresentationInformation().generate();
        return this;
    }

}
