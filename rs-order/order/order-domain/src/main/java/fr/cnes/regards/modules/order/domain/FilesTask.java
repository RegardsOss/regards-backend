package fr.cnes.regards.modules.order.domain;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.modules.jobs.domain.LeafTask;

/**
 * A sub-order task is a job that manage a set of data files.
 * Associated job calls
 * @author oroussel
 */
@Entity
@Table(name = "t_files_task")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "fk_task_id"))
public class FilesTask extends LeafTask {

    @OneToMany // dataFiles used on more than one DataObjects are considered as not identical.
    @JoinColumn(name = "files_task_id", foreignKey = @ForeignKey(name = "fk_files_task"))
    private Set<OrderDataFile> files = new HashSet<>();

    public Set<OrderDataFile> getFiles() {
        return files;
    }

    public void addFile(OrderDataFile orderDataFile) {
        this.files.add(orderDataFile);
    }

    /**
     * Use a defensive copy to add DataFile
     */
    public void addAllFiles(Collection<OrderDataFile> files) {
        files.forEach(this::addFile);
    }

}
