/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain.project;

import java.util.List;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class ModuleConfiguration {

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "navCtxProjectSequence")
    private Long id;

    private Project project;

    private List<ConfigParameter> parameters;

    private Module module;

    /**
     * Default constructor
     */
    public ModuleConfiguration() {
        super();
    }

    /**
     * A constructor using fields.
     * 
     * @param pProject
     *            the project
     * @param pParameters
     *            a list of {@link ConfigParameter}
     * @param pModule
     *            a {@link Module}
     */
    public ModuleConfiguration(Project pProject, List<ConfigParameter> pParameters, Module pModule) {
        super();
        project = pProject;
        parameters = pParameters;
        module = pModule;
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project pProject) {
        project = pProject;
    }

    public List<ConfigParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ConfigParameter> pParameters) {
        parameters = pParameters;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module pModule) {
        module = pModule;
    }

}
