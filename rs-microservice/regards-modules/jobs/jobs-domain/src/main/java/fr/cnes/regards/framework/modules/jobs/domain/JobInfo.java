/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.utils.RsRuntimeException;

/**
 * Store Job Information
 * @author oroussel
 */
@Entity
@Table(name = "t_job_info")
public class JobInfo {

    /**
     * JobInfo id. An UUID is used to be unique because job service manages several jobs from several multitenants
     * so several databases
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private UUID id;

    /**
     * Job priority
     */
    @Column(name = "priority")
    private Integer priority = 0;

    /**
     * Date when the job should be expired
     */
    @Column(name = "expire_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime expirationDate;

    /**
     * Job result (nullable)
     */
    @Column
    @Type(type = "text")
    private String result;

    /**
     * Job result class name
     */
    @Column(name = "result_class_name", length = 255)
    private String resultClassName;

    /**
     * Job parameters
     */
    @ElementCollection
    @CollectionTable(name = "t_job_parameters", joinColumns = @JoinColumn(name = "job_id"),
            foreignKey = @ForeignKey(name = "fk_job_param"))
    private Set<JobParameter> parameters = new HashSet<>();

    /**
     * Job owner (its email)
     */
    @Column(name = "owner")
    private String owner;

    /**
     * Job class to execute
     */
    @Column(name = "class_name", length = 255)
    private String className;

    /**
     * locked column permits to inform the business user of jobs (ie microservice developer) that this job is used by
     * another entity (ie has a foreign key on it). Locked jobs are not cleaned by automatic cleaning process
     * (cf. JobInfoService)
     */
    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    /**
     * Date of the last completion compute
     */
    @Column(name = "last_completion_update")
    private OffsetDateTime lastCompletionUpdate;

    /**
     * Field characteristics of this job. Saved on cascade
     */
    @Embedded
    private final JobStatusInfo status = new JobStatusInfo();

    @Transient
    private String tenant;

    @SuppressWarnings("rawtypes")
    @Transient
    private IJob job;

    /**
     * Default constructor only used by Hb9
     */
    @SuppressWarnings("unused")
    private JobInfo() {
        super();
    }

    /**
     * locked parameter permits to inform that this job is used by another entity (ie has a foreign key on it).
     * Locked jobs are not cleaned by automatic cleaning process (cf. JobInfoService)
     */
    public JobInfo(boolean locked) {
        this.locked = locked;
    }

    /**
     * locked parameter permits to inform that this job is used by another entity (ie has a foreign key on it).
     * Locked jobs are not cleaned by automatic cleaning process (cf. JobInfoService)
     */
    public JobInfo(boolean locked, Integer priority, Set<JobParameter> parameters, String owner, String className) {
        this(locked);
        this.priority = priority;
        this.parameters = parameters;
        this.owner = owner;
        this.className = className;
    }

    public void updateStatus(JobStatus status) {
        this.status.setStatus(status);
        switch (status) {
            case QUEUED:
                this.status.setQueuedDate(OffsetDateTime.now());
            case PENDING:
            case TO_BE_RUN:
                this.status.setPercentCompleted(0);
                break;
            case RUNNING:
                this.status.setStartDate(OffsetDateTime.now());
                break;
            case FAILED:
            case ABORTED:
                this.status.setStopDate(OffsetDateTime.now());
                break;
            case SUCCEEDED:
                this.status.setStopDate(OffsetDateTime.now());
                this.status.setPercentCompleted(100);
                break;
            default:
        }
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setParameters(Set<JobParameter> parameters) {
        this.parameters = parameters;
    }

    public void setParameters(JobParameter... params) {
        setParameters(Sets.newHashSet(params));
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public <T> T getResult() {
        try {
            return this.resultClassName == null ? null : GsonUtil.fromString(result, Class.forName(resultClassName));
        } catch (ClassNotFoundException e) {
            throw new RsRuntimeException(e);
        }
    }

    public void setResult(Object result) {
        if (result != null) {
            this.resultClassName = result.getClass().getName();
        }
        this.result = GsonUtil.toString(result);
    }

    public Set<JobParameter> getParameters() {
        return parameters;
    }

    /**
     * @return a non null parameter {@link Map} with key representing the parameter name and value the
     * {@link JobParameter}
     */
    public Map<String, JobParameter> getParametersAsMap() {
        Map<String, JobParameter> parameterMap = new HashMap<>();
        if (parameters != null) {
            parameters.forEach(param -> parameterMap.put(param.getName(), param));
        }
        return parameterMap;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
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
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public JobStatusInfo getStatus() {
        return status;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    @SuppressWarnings("rawtypes")
    public IJob getJob() {
        return job;
    }

    @SuppressWarnings("rawtypes")
    public void setJob(IJob job) {
        this.job = job;
        if (this.job instanceof AbstractJob) {
            ((AbstractJob) this.job).addObserver(this.status);
        }
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public OffsetDateTime getLastCompletionUpdate() {
        return lastCompletionUpdate;
    }

    public void setLastCompletionUpdate(OffsetDateTime lastCompletionUpdate) {
        this.lastCompletionUpdate = lastCompletionUpdate;
    }

    /**
     * A JobInfo has no business key but is immediately created or retrieved from Database before be used so we can
     * freely use its id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        JobInfo jobInfo = (JobInfo) o;

        return id != null ? id.equals(jobInfo.id) : jobInfo.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "JobInfo{" + "id=" + id + ", priority=" + priority + ", expirationDate=" + expirationDate + ", owner='"
                + owner + '\'' + ", className='" + className + '\'' + ", status=" + status + ", tenant='" + tenant
                + '\'' + '}';
    }
}
