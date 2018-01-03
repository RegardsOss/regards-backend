package fr.cnes.regards.framework.oais;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.adapter.InformationPackageMap;

/**
 * Information package main structure
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class InformationPackageProperties {

    /**
     * The content informations
     */
    @NotEmpty(message = "At least one content information is required")
    @Valid
    private Set<ContentInformation> contentInformations;

    /**
     * The preservation and description information
     */
    @NotNull(message = "Preservation description information is required")
    @Valid
    private PreservationDescriptionInformation pdi = new PreservationDescriptionInformation();

    /**
     * The descriptive information
     */
    private InformationPackageMap descriptiveInformation;

    /**
     * @return the content information
     */
    public Set<ContentInformation> getContentInformations() {
        if (contentInformations == null) {
            contentInformations = Sets.newHashSet();
        }
        return contentInformations;
    }

    /**
     * @return the preservation and description information
     */
    public PreservationDescriptionInformation getPdi() {
        return pdi;
    }

    /**
     * Set the preservation and description information
     * @param pPdi
     */
    public void setPdi(PreservationDescriptionInformation pPdi) {
        pdi = pPdi;
    }

    /**
     * @return the descriptive information
     */
    public Map<String, Object> getDescriptiveInformation() {
        if (descriptiveInformation == null) {
            descriptiveInformation = new InformationPackageMap();
        }
        return descriptiveInformation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((contentInformations == null) ? 0 : contentInformations.hashCode());
        result = (prime * result) + ((descriptiveInformation == null) ? 0 : descriptiveInformation.hashCode());
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
        InformationPackageProperties other = (InformationPackageProperties) obj;
        if (contentInformations == null) {
            if (other.contentInformations != null) {
                return false;
            }
        } else if (!contentInformations.equals(other.contentInformations)) {
            return false;
        }
        if (descriptiveInformation == null) {
            if (other.descriptiveInformation != null) {
                return false;
            }
        } else if (!descriptiveInformation.equals(other.descriptiveInformation)) {
            return false;
        }
        if (pdi == null) {
            if (other.pdi != null) {
                return false;
            }
        } else if (!pdi.equals(other.pdi)) {
            return false;
        }
        return true;
    }
}
