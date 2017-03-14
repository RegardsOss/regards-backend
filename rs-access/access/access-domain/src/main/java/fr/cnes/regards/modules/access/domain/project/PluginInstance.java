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
public class PluginInstance {

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "navCtxProjectSequence")
    private Long id;

    private String name;

    private Project project;

    private List<ConfigParameter> parameters;

    /**
     * Default constructor
     */
    public PluginInstance() {
        super();
    }

    /**
     * A constructor using fields.
     * 
     * @param pName
     *            a name
     * @param pProject
     *            a {@link Project}
     * @param pParameters
     *            a list of {@link ConfigParameter}
     */
    public PluginInstance(String pName, Project pProject, List<ConfigParameter> pParameters) {
        super();
        name = pName;
        project = pProject;
        parameters = pParameters;
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

}
