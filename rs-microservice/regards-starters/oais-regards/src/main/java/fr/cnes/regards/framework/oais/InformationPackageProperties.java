package fr.cnes.regards.framework.oais;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class InformationPackageProperties {

    private EntityType ip_type;

    @NotEmpty
    private Set<ContentInformation> contentInformations;

    @NotNull
    private PreservationDescriptionInformation pdi = new PreservationDescriptionInformation();

    private Map<String, Object> descriptiveInformation;

    public Set<ContentInformation> getContentInformations() {
        if(contentInformations == null) {
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
        return descriptiveInformation;
    }

    public EntityType getIpType() {
        return ip_type;
    }

    public void setIpType(EntityType ip_type) {
        this.ip_type = ip_type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InformationPackageProperties that = (InformationPackageProperties) o;

        if (ip_type != that.ip_type) {
            return false;
        }
        if (!contentInformations.equals(that.contentInformations)) {
            return false;
        }
        if (!pdi.equals(that.pdi)) {
            return false;
        }
        return descriptiveInformation != null ?
                descriptiveInformation.equals(that.descriptiveInformation) :
                that.descriptiveInformation == null;
    }

    @Override
    public int hashCode() {
        int result = ip_type != null ? ip_type.hashCode() : 0;
        result = 31 * result + contentInformations.hashCode();
        result = 31 * result + pdi.hashCode();
        result = 31 * result + (descriptiveInformation != null ? descriptiveInformation.hashCode() : 0);
        return result;
    }
}
