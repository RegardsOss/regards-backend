/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import java.util.Optional;

/**
 * Class ProjectConnection
 * <p>
 * ProjectConnection Entity. Describe a database connection for a couple project/microservice
 *
 * @author CS
 */
@ValidateOnExecution
@InstanceEntity
@Entity
@SequenceGenerator(name = "projectConnectionSequence", initialValue = 1, sequenceName = "seq_project_connection")
@Table(name = "t_project_connection",
       uniqueConstraints = { @UniqueConstraint(name = "uk_project_connection_project_microservice",
                                               columnNames = { "project_id", "microservice" }),
                             @UniqueConstraint(name = "uk_t_project_connection_microservice_url",
                                               columnNames = { "microservice", "url" }) })
public class ProjectConnection implements IIdentifiable<Long> {

    public static final int ERROR_MAX_LENGTH = 255;

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
    @Column(name = "cause", length = ERROR_MAX_LENGTH)
    private String errorCause;

    /**
     * Constructor
     */
    public ProjectConnection() {
        microservice = "undefined";
        project = new Project();
    }

    /**
     * Constructor
     *
     * @param project         Associated project
     * @param microservice    Microservice name
     * @param username        Database username
     * @param password        Database password
     * @param driverClassName Database driver class name
     * @param url             Database url
     */
    public ProjectConnection(Project project,
                             String microservice,
                             String username,
                             String password,
                             String driverClassName,
                             String url) {
        super();
        this.project = project;
        this.microservice = microservice;
        userName = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.url = url;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getMicroservice() {
        return microservice;
    }

    public void setMicroservice(String microservice) {
        this.microservice = microservice;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String username) {
        userName = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String pUrl) {
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
     * Transform a ProjectConnection in TenantConnection
     *
     * @return TenantConnection
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
