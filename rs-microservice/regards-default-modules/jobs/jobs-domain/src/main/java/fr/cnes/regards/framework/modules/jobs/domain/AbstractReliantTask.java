package fr.cnes.regards.framework.modules.jobs.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import java.util.HashSet;
import java.util.Set;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Reliant task. Associated jobs of this task must not be executed while reliants task jobs are not terminated
 * This class is abstract. Please inherit it from your microservice adding your personal informations in order to use it
 * @author oroussel
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractReliantTask<K extends AbstractReliantTask> implements IIdentifiable<Long> {
    @Id
    @SequenceGenerator(name = "EntitySequence", initialValue = 1, sequenceName = "seq_entity")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EntitySequence")
    protected Long id;

    @OneToMany
    @JoinTable(name = "ta_task_job_infos")
    protected Set<JobInfo> jobInfos = new HashSet<>();

    @ManyToMany(targetEntity = AbstractReliantTask.class)
    @JoinTable(name = "ta_tasks_reliant_tasks")
//    protected Set<? extends AbstractReliantTask> reliantTasks = new HashSet<>();
    protected Set<K> reliantTasks = new HashSet<>();


    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    public Set<JobInfo> getJobInfos() {
        return jobInfos;
    }

    public void setJobInfos(Set<JobInfo> jobInfos) {
        this.jobInfos = jobInfos;
    }

    public Set<K> getReliantTasks() {
        return reliantTasks;
    }

    public void setReliantTasks(Set<K> reliantTasks) {
        this.reliantTasks = reliantTasks;
    }
}
