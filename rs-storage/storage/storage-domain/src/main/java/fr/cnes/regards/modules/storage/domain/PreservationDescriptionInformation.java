/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.validation.constraints.NotNull;

public class PreservationDescriptionInformation implements Serializable {

    @NotNull
    private AccessRightInformation accessRightInformation;

    private Map<String, Object> contextInformation;

    @NotNull
    private FixityInformation fixityInformation;

    @NotNull
    private ProvenanceInformation provenanceInformation;

    public PreservationDescriptionInformation(AccessRightInformation pAccesRightInformation,
            FixityInformation pFixityInformation, ProvenanceInformation pProvenanceInformation) {
        super();
        accessRightInformation = pAccesRightInformation;
        fixityInformation = pFixityInformation;
        provenanceInformation = pProvenanceInformation;
    }

    /**
     * to be used with generate method
     */
    public PreservationDescriptionInformation() {

    }

    public AccessRightInformation getAccesRightInformation() {
        return accessRightInformation;
    }

    public void setAccesRightInformation(AccessRightInformation pAccesRightInformation) {
        accessRightInformation = pAccesRightInformation;
    }

    public Map<String, Object> getContextInformation() {
        return contextInformation;
    }

    public void setContextInformation(Map<String, Object> pContextInformation) {
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
        accessRightInformation = new AccessRightInformation().generate();
        fixityInformation = new FixityInformation().generate();
        provenanceInformation = new ProvenanceInformation().generate();
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((accessRightInformation == null) ? 0 : accessRightInformation.hashCode());
        result = (prime * result) + ((contextInformation == null) ? 0 : contextInformation.hashCode());
        result = (prime * result) + ((fixityInformation == null) ? 0 : fixityInformation.hashCode());
        result = (prime * result) + ((provenanceInformation == null) ? 0 : provenanceInformation.hashCode());
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
        PreservationDescriptionInformation other = (PreservationDescriptionInformation) obj;
        if (accessRightInformation == null) {
            if (other.accessRightInformation != null) {
                return false;
            }
        } else
            if (!accessRightInformation.equals(other.accessRightInformation)) {
                return false;
            }
        if (contextInformation == null) {
            if (other.contextInformation != null) {
                return false;
            }
        } else
            if (!contextInformation.equals(other.contextInformation)) {
                return false;
            }
        if (fixityInformation == null) {
            if (other.fixityInformation != null) {
                return false;
            }
        } else
            if (!fixityInformation.equals(other.fixityInformation)) {
                return false;
            }
        if (provenanceInformation == null) {
            if (other.provenanceInformation != null) {
                return false;
            }
        } else
            if (!provenanceInformation.equals(other.provenanceInformation)) {
                return false;
            }
        return true;
    }

}
