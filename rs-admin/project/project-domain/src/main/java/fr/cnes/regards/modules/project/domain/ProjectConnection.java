/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.project.domain;

import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;

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
@Entity
@SequenceGenerator(name = "projectConnectionSequence", initialValue = 1, sequenceName = "seq_project_connection")
@Table(name = "t_project_connection",
        uniqueConstraints = { @UniqueConstraint(name = "uk_project_connection_project_microservice",
                columnNames = { "project_id", "microservice" }) })
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
    @JoinColumn(name = "project_id", foreignKey = @javax.persistence.ForeignKey(name = "fk_project_connection"))
    @NotNull
    private Project project;

    /**
     * Microservice name
     */
    @Column(name = "microservice", nullable = false, length = 50)
    @NotNull
    private String microservice;

    /**
     * Database username
     */
    @Column(name = "userName", nullable = false, length = 30)
    @NotNull
    private String userName;

    /**
     * Database password
     */
    @Column(name = "password", nullable = false, length = 255)
    @NotNull
    private String password;

    /**
     * Database driver class name
     */
    @Column(name = "driverClassName", nullable = false, length = 200)
    @NotNull
    private String driverClassName;

    /**
     * Database URL
     */
    @Column(name = "url", nullable = false, length = 255)
    @NotNull
    private String url;

    /**
     * Manage connection life cycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantConnectionState state = TenantConnectionState.DISABLED;

    /**
     * If {@link TenantConnectionState#ERROR}, explains the error cause
     */
    @Column(name = "cause", length = 255)
    private String errorCause;

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

    public TenantConnectionState getState() {
        return state;
    }

    public void setState(TenantConnectionState state) {
        this.state = state;
    }

    public Optional<String> getErrorCause() {
        return Optional.ofNullable(errorCause);
    }

    public void setErrorCause(String errorCause) {
        this.errorCause = errorCause;
    }

    /**
     * Transform a {@link ProjectConnection} in {@link TenantConnection}
     *
     * @param projectConnection
     *            {@link ProjectConnection}
     * @return {@link TenantConnection}
     */
    public TenantConnection toTenantConnection() {
        TenantConnection tenantConnection = new TenantConnection();
        tenantConnection.setDriverClassName(driverClassName);
        tenantConnection.setTenant(project.getName());
        tenantConnection.setPassword(password);
        tenantConnection.setUrl(url);
        tenantConnection.setUserName(userName);
        tenantConnection.setState(state);
        tenantConnection.setErrorCause(errorCause);
        return tenantConnection;
    }
}
