/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

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
import javax.validation.constraints.NotNull;

import fr.cnes.regards.modules.jobs.domain.converters.JobParameterConverter;
import fr.cnes.regards.modules.jobs.domain.converters.PathConverter;

@Entity(name = "T_JOB")
public class Job implements IJob {

    @NotNull
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "job_sequence", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "job_sequence", allocationSize = 10)
    private Long id;

    @Column(name = "priority")
    private int priority;

    @Column(name = "workspace", columnDefinition = "LONGVARCHAR")
    @Convert(converter = PathConverter.class)
    private Path workspace;

    /**
     * Job result (nullable)
     */

    // private List<Output> result;

    @Column(name = "parameters", columnDefinition = "LONGVARCHAR")
    @Convert(converter = JobParameterConverter.class)
    private JobParameters parameters;

    @Column(name = "owner")
    private String owner;

    @Column(name = "className")
    private String className;

    /**
     * Hield caracteristics of this job. Saved on cascade
     */
    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "status_id", foreignKey = @ForeignKey(name = "FK_JOB_STATUS_INFO"))
    private StatusInfo status;

    public Job() {
        super();
    }

    public Job(JobConfiguration pJobConfiguration) {
        this();
        className = pJobConfiguration.getClassName();
        parameters = pJobConfiguration.getParameters();
        owner = pJobConfiguration.getOwner();
        status = pJobConfiguration.getStatusInfo();
        workspace = pJobConfiguration.getWorkspace();
        priority = pJobConfiguration.getPriority();
    }

    /**
     * @param pId
     *            the id to set
     */
    public void setId(Long pId) {
        id = pId;
    }

    /**
     * @param pPriority
     *            the priority to set
     */
    public void setPriority(int pPriority) {
        priority = pPriority;
    }

    /**
     * @param pParameters
     *            the parameters to set
     */
    public void setParameters(JobParameters pParameters) {
        parameters = pParameters;
    }

    /**
     * @param pOwner
     *            the owner to set
     */
    public void setOwner(String pOwner) {
        owner = pOwner;
    }

    /**
     * @return the result
     */
    public List<Output> getResult() {
        return null;
    }

    /**
     * @param pResult
     *            the result to set
     */
    public void setResult(List<Output> pResult) {

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
    public void setClassName(String pClassName) {
        className = pClassName;
    }

    /**
     * @param pStatus
     *            the status to set
     */
    public void setStatus(StatusInfo pStatus) {
        status = pStatus;
    }

    @Override
    public final List<Output> getResults() {
        return null;
    }

    @Override
    public final int getPriority() {
        return this.priority;
    }

    @Override
    public void setWorkspace(Path pWorkspace) {
        workspace = pWorkspace;
    }

    @Override
    public StatusInfo getStatus() {
        return status;
    }

    @Override
    public StatusInfo cancel() {
        return null;
    }

    @Override
    public StatusInfo execute() {
        return null;
    }

    @Override
    public boolean hasResult() {
        return false;
    }

    @Override
    public boolean needWorkspace() {
        return false;
    }

    @Override
    public StatusInfo restart() {
        return null;
    }

    @Override
    public StatusInfo stop() {
        return null;
    }

    @Override
    public Long getId() {
        return id;
    }

}
