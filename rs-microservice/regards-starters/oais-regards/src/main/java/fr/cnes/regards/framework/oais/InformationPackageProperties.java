package fr.cnes.regards.framework.oais;

import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class InformationPackageProperties {

    @NotEmpty
    private Set<ContentInformation> contentInformations;

    @NotNull
    private PreservationDescriptionInformation pdi = new PreservationDescriptionInformation();

    private Map<String, Object> descriptiveInformation;

    public Set<ContentInformation> getContentInformations() {
        if (contentInformations == null) {
            contentInformations = Sets.newHashSet();
        }
        return contentInformations;
    }

    public PreservationDescriptionInformation getPdi() {
        return pdi;
    }

    public void setPdi(PreservationDescriptionInformation pPdi) {
        pdi = pPdi;
    }

    public Map<String, Object> getDescriptiveInformation() {
        if (descriptiveInformation == null) {
            descriptiveInformation = Maps.newHashMap();
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
