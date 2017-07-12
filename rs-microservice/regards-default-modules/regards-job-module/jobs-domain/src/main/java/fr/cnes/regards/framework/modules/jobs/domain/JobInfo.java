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
package fr.cnes.regards.framework.modules.jobs.domain;

import java.nio.file.Path;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.modules.jobs.domain.converters.JobOuputConverter;
import fr.cnes.regards.framework.modules.jobs.domain.converters.PathConverter;

/**
 * Store Job Information
 *
 * @author Léo Mieulet
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_job_info")
@SequenceGenerator(name = "jobInfoSequence", initialValue = 1, sequenceName = "seq_job_info")
public class JobInfo implements IIdentifiable<Long> {

    /**
     * JobInfo id
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jobInfoSequence")
    private Long id;

    /**
     * Job priority
     */
    @Column(name = "priority")
    private Integer priority;

    /**
     * Job workspace
     */
    @Column(name = "workspace", columnDefinition = "LONGVARCHAR")
    @Convert(converter = PathConverter.class)
    private Path workspace;

    /**
     * Job result (nullable)
     */
    @Column(name = "result", columnDefinition = "LONGVARCHAR")
    @Convert(converter = JobOuputConverter.class)
    private List<Output> result;

    /**
     * Job parameters
     */
    @Column(name = "parameters", columnDefinition = "LONGVARCHAR")
    private JobParameters parameters;

    /**
     * Job owner
     */
    @Column(name = "owner")
    private String owner;

    /**
     * Job class to execute
     */
    @Column(name = "className")
    private String className;

    /**
     * Field characteristics of this job. Saved on cascade
     */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "status_info", foreignKey = @ForeignKey(name = "FK_JOB_STATUS_INFO"))
    private StatusInfo status;

    /**
     * When true, the job is done, results deleted, job metadata archived
     */
    @Column(name = "archived")
    private Boolean archived;

    /**
     * Default constructor
     */
    public JobInfo() {
        super();
    }

    /**
     *
     * @param pJobConfiguration
     *            the {@link JobConfiguration} to used
     */
    public JobInfo(final JobConfiguration pJobConfiguration) {
        this();
        className = pJobConfiguration.getClassName();
        parameters = pJobConfiguration.getParameters();
        owner = pJobConfiguration.getOwner();
        status = pJobConfiguration.getStatusInfo();
        workspace = pJobConfiguration.getWorkspace();
        priority = pJobConfiguration.getPriority();
        archived = Boolean.FALSE;
    }

    /**
     * @param pId
     *            the id to set
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     *
     * @return job id
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * @param pPriority
     *            the priority to set
     */
    public void setPriority(final int pPriority) {
        priority = Integer.valueOf(pPriority);
    }

    /**
     * @param pParameters
     *            the parameters to set
     */
    public void setParameters(final JobParameters pParameters) {
        parameters = pParameters;
    }

    /**
     * @param pOwner
     *            the owner to set
     */
    public void setOwner(final String pOwner) {
        owner = pOwner;
    }

    /**
     * @return the result
     */
    public List<Output> getResult() {
        return result;
    }

    /**
     * @param pResult
     *            the result to set
     */
    public void setResult(final List<Output> pResult) {
        result = pResult;
    }

    /**
     * @return the parameters
     */
    public JobParameters getParameters() {
        return parameters;
    }

    /**
     * A specific parameter
     *
     * @return the workspace, where files are located
     */
    public Path getWorkspace() {
        return workspace;
    }

    /**
     *
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @param pClassName
     *            the className to set
     */
    public void setClassName(final String pClassName) {
        className = pClassName;
    }

    /**
     * @param pStatus
     *            the status to set
     */
    public void setStatus(final StatusInfo pStatus) {
        status = pStatus;
    }

    public final Integer getPriority() {
        return this.priority;
    }

    public void setWorkspace(final Path pWorkspace) {
        workspace = pWorkspace;
    }

    public StatusInfo getStatus() {
        return status;
    }

    /**
     * Is the job need a workspace ?
     * @return true/false
     */
    public boolean needWorkspace() {
        return workspace != null;
    }

    /**
     * @return job is archived
     */
    public Boolean isArchived() {
        return archived;
    }

    /**
     * @param pArchived
     *            set if job archived
     */
    public void setArchived(final boolean pArchived) {
        archived = Boolean.valueOf(pArchived);
    }

}
