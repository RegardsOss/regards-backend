/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;

import javax.validation.constraints.NotNull;

public class InformationObject implements Serializable {

    @NotNull
    private ContentInformation contentInformation;

    @NotNull
    private PreservationDescriptionInformation pdi;

    public InformationObject() {
    }

    public InformationObject(ContentInformation pContentInformation, PreservationDescriptionInformation pPdi) {
        super();
        contentInformation = pContentInformation;
        pdi = pPdi;
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

    public InformationObject generateRandomInformationObject() throws NoSuchAlgorithmException, MalformedURLException {
        contentInformation = new ContentInformation().generate();
        pdi = new PreservationDescriptionInformation().generate();
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((contentInformation == null) ? 0 : contentInformation.hashCode());
        result = (prime * result) + ((pdi == null) ? 0 : pdi.hashCode());
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
        InformationObject other = (InformationObject) obj;
        if (contentInformation == null) {
            if (other.contentInformation != null) {
                return false;
            }
        } else
            if (!contentInformation.equals(other.contentInformation)) {
                return false;
            }
        if (pdi == null) {
            if (other.pdi != null) {
                return false;
            }
        } else
            if (!pdi.equals(other.pdi)) {
                return false;
            }
        return true;
    }

}
