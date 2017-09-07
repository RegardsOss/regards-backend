/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.net.MalformedURLException;

public class ContentInformation implements Serializable {

    @NotNull
    private DataObject dataObject;

    @NotNull
    private RepresentationInformation representationInformation;

    public ContentInformation(DataObject dataObject, RepresentationInformation representationInformation) {
        this.dataObject = dataObject;
        this.representationInformation = representationInformation;
    }

    /**
     *
     */
    public ContentInformation() {
        // TODO Auto-generated constructor stub
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((dataObject == null) ? 0 : dataObject.hashCode());
        result = (prime * result) + ((representationInformation == null) ? 0 : representationInformation.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContentInformation other = (ContentInformation) obj;
        if (dataObject == null) {
            if (other.dataObject != null) {
                return false;
            }
        } else if (!dataObject.equals(other.dataObject)) {
            return false;
        }
        if (representationInformation == null) {
            if (other.representationInformation != null) {
                return false;
            }
        } else if (!representationInformation.equals(other.representationInformation)) {
            return false;
        }
        return true;
    }

}
