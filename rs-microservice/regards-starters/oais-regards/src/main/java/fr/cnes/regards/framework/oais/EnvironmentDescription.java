package fr.cnes.regards.framework.oais;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * TODO: please add some doc on what information can/should/should not be there
 * when the todo was added noone in the team had any idea of which kind of information should be there!!!!
 * @author Sylvain VISSIERE-GUERINET
 */
public class EnvironmentDescription {

    private final Map<String, Object> softwareEnvironment;

    private final Map<String, Object> hardwareEnvironment;

    public EnvironmentDescription() {
        softwareEnvironment = Maps.newHashMap();
        hardwareEnvironment = Maps.newHashMap();
    }

    public Map<String, Object> getSoftwareEnvironment() {
        return softwareEnvironment;
    }

    public Map<String, Object> getHardwareEnvironment() {
        return hardwareEnvironment;
    }

}
