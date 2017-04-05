/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;

/**
 *
 * Class ProjectConnection
 *
 * ProjectConnection Entity. Describe a database connection for a couple project/microservice
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ValidateOnExecution
@InstanceEntity
@Entity(name = "T_PROJECT_CONNECTION")
@SequenceGenerator(name = "projectConnectionSequence", initialValue = 1, sequenceName = "SEQ_PROJECT_CONNECTION")
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "project_id", "microservice" }) })
public class ProjectConnection implements IIdentifiable<Long> {

    /**
     * Identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projectConnectionSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Associated Project
     */
    @ManyToOne
    @JoinColumn(name = "project_id", foreignKey = @javax.persistence.ForeignKey(name = "FK_PROJECT_CONNECTION"))
    @NotNull
    private Project project;

    /**
     * Microservice name
     */
    @Column(name = "microservice", nullable = false)
    @NotNull
    private String microservice;

    /**
     * Database username
     */
    @Column(name = "userName", nullable = false)
    @NotNull
    private String userName;

    /**
     * Database password
     */
    @Column(name = "password", nullable = false)
    @NotNull
    private String password;

    /**
     * Database driver class name
     */
    @Column(name = "driverClassName", nullable = false)
    @NotNull
    private String driverClassName;

    /**
     * Database URL
     */
    @Column(name = "url", nullable = false)
    @NotNull
    private String url;

    /**
     *
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    public ProjectConnection() {
        super();
        microservice = "undefined";
        project = new Project();
    }

    /**
     *
     * Constructor
     *
     * @param pId
     *            Identifier
     * @param pProject
     *            Associated project
     * @param pMicroservice
     *            Microservice name
     * @param pUserName
     *            Database username
     * @param pPassword
     *            Database password
     * @param pDriverClassName
     *            Database driver class name
     * @param pUrl
     *            Database url
     * @since 1.0-SNAPSHOT
     */
    public ProjectConnection(final Long pId, final Project pProject, final String pMicroservice, final String pUserName,
            final String pPassword, final String pDriverClassName, final String pUrl) {
        super();
        id = pId;
        project = pProject;
        microservice = pMicroservice;
        userName = pUserName;
        password = pPassword;
        driverClassName = pDriverClassName;
        url = pUrl;
    }

    /**
     *
     * Constructor
     *
     * @param pProject
     *            Associated project
     * @param pMicroservice
     *            Microservice name
     * @param pUserName
     *            Database username
     * @param pPassword
     *            Database password
     * @param pDriverClassName
     *            Database driver class name
     * @param pUrl
     *            Database url
     * @since 1.0-SNAPSHOT
     */
    public ProjectConnection(final Project pProject, final String pMicroservice, final String pUserName,
            final String pPassword, final String pDriverClassName, final String pUrl) {
        super();
        project = pProject;
        microservice = pMicroservice;
        userName = pUserName;
        password = pPassword;
        driverClassName = pDriverClassName;
        url = pUrl;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(final Project pProject) {
        project = pProject;
    }

    public String getMicroservice() {
        return microservice;
    }

    public void setMicroservice(final String pMicroservice) {
        microservice = pMicroservice;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String pUserName) {
        userName = pUserName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String pPassword) {
        password = pPassword;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(final String pDriverClassName) {
        driverClassName = pDriverClassName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String pUrl) {
        url = pUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean pEnabled) {
        enabled = pEnabled;
    }

}
