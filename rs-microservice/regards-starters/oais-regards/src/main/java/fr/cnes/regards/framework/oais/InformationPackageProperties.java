package fr.cnes.regards.framework.oais;

import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class InformationPackageProperties {

    private EntityType ip_type;

    private InformationPackage informationPackage;

    public EntityType getIp_type() {
        return ip_type;
    }

    public void setIp_type(EntityType ip_type) {
        this.ip_type = ip_type;
    }

    public InformationPackage getInformationPackage() {
        return informationPackage;
    }

    public void setInformationPackage(InformationPackage informationPackage) {
        this.informationPackage = informationPackage;
    }
}
