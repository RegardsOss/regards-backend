/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;

/**
 * Store Job Information
 * @author Léo Mieulet
 * @author Christophe Mertz
 */
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Entity
@Table(name = "t_job_info")
@SequenceGenerator(name = "jobInfoSequence", sequenceName = "seq_job_info")
public class JobInfo {

    /**
     * JobInfo id
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jobInfoSequence")
    private UUID id;

    /**
     * Job priority
     */
    @Column(name = "priority")
    private Integer priority;

    /**
     * Job workspace
     */
    @Column
    @Type(type = "text")
    private String workspace;

    /**
     * Job description
     */
    @Column(name = "description")
    @Type(type = "text")
    private String description;

    /**
     * Date when the job should be expired
     */
    @Column(name = "expirationDate")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime expirationDate;

    /**
     * Job results (nullable)
     */
    @ElementCollection
    @CollectionTable(name = "t_job_result", joinColumns = @JoinColumn(name = "job_id"),
            foreignKey = @ForeignKey(name = "fk_job_result"))
    private Set<JobResult> results = new HashSet<>();

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
    @Column(name = "className")
    private String className;

    /**
     * Field characteristics of this job. Saved on cascade
     */
    @Embedded
    private JobStatusInfo status = new JobStatusInfo();

    /**
     * Default constructor
     */
    public JobInfo() {
        super();
    }

    public JobInfo(Integer priority, String workspace, Set<JobParameter> parameters, String owner, String className,
            JobStatusInfo status) {
        this.priority = priority;
        this.workspace = workspace;
        this.parameters = parameters;
        this.owner = owner;
        this.className = className;
        this.status = status;
    }

    public void updateStatus(JobStatus status) {
        this.status.setStatus(status);
        switch (status) {
            case PENDING:
                this.status.setPercentCompleted(0);
                break;
            case RUNNING:
                this.status.setStartDate(OffsetDateTime.now());
                break;
            case FAILED:
            case ABORTED:
            case SUCCEEDED:
                this.status.setStopDate(OffsetDateTime.now());
                break;
            default:
        }
    }

    public void setId(UUID pId) {
        id = pId;
    }

    public UUID getId() {
        return id;
    }

    public void setPriority(int pPriority) {
        priority = Integer.valueOf(pPriority);
    }

    public void setParameters(Set<JobParameter> parameters) {
        this.parameters = parameters;
    }

    public void setOwner(String pOwner) {
        owner = pOwner;
    }

    public Set<JobResult> getResults() {
        return results;
    }

    public void setResults(Set<JobResult> pResult) {
        results = pResult;
    }

    public Set<JobParameter> getParameters() {
        return parameters;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * A specific parameter
     * @return the workspace, where files are located
     */
    public Path getWorkspace() {
        return (workspace == null) ? null : Paths.get(workspace);
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
     * @param pClassName the className to set
     */
    public void setClassName(String pClassName) {
        className = pClassName;
    }

    /**
     * @param pStatus the status to set
     */
    public void setStatus(JobStatusInfo pStatus) {
        status = pStatus;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public void setWorkspace(Path workspace) {
        this.workspace = (workspace == null) ? null : workspace.toString();
    }

    public JobStatusInfo getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Is the job need a workspace ?
     * @return true/false
     */
    public boolean needWorkspace() {
        return workspace != null;
    }

    /**
     * A job has no business key
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobInfo jobInfo = (JobInfo) o;

        return id != null ? id.equals(jobInfo.id) : jobInfo.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
