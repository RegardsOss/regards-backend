package fr.cnes.regards.framework.microservice.rest.test.domain;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class ConfigurationPojo {

    private String attribute;

    public ConfigurationPojo(String attribute) {
        this.attribute = attribute;
    }

    protected ConfigurationPojo() {
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }
}
