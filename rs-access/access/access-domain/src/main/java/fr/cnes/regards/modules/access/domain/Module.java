/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

import java.util.List;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * 
 * @author Christophe Mertz
 *
 */
// @Entity
// @Table(name = "T_ACCESS_TDOMAIN")
// @SequenceGenerator(name = "accessDomainSequence", initialValue = 1, sequenceName = "SEQ_ACCESS_DOMAIN")
public class Module {

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accessDomainSequence")
    private Long id;

    private String name;

    private String description;

    private List<ModuleConfiguration> configurations;

    private ModuleType moduleType;

    /**
     * Default constructor
     */
    public Module() {
        super();
    }

    /**
     * A constructor using fields.
     * 
     * @param pName
     *            the name
     * @param pDescription
     *            the description
     * @param pConfigurations
     *            a list of {@link ModuleConfiguration}
     * @param pModuleType
     *            a {@link ModuleType}
     */
    public Module(String pName, String pDescription, List<ModuleConfiguration> pConfigurations,
            ModuleType pModuleType) {
        super();
        name = pName;
        description = pDescription;
        configurations = pConfigurations;
        moduleType = pModuleType;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public List<ModuleConfiguration> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<ModuleConfiguration> pConfigurations) {
        configurations = pConfigurations;
    }

    public ModuleType getModuleType() {
        return moduleType;
    }

    public void setModuleType(ModuleType pModuleType) {
        moduleType = pModuleType;
    }

}
