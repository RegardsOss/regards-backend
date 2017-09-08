package fr.cnes.regards.modules.order.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractReliantTask;
import fr.cnes.regards.framework.modules.jobs.domain.LeafTask;

/**
 * A sub-order task is a job that manage a set of data files.
 * Associated job calls
 * @author oroussel
 */
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Entity
@Table(name = "t_files_task")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "fk_task_id"))
public class FilesTask extends LeafTask {

    @Column(name = "files", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    private Set<StatedDataFile> files = new HashSet<>();

    public Set<StatedDataFile> getFiles() {
        return files;
    }

    public void addFile(StatedDataFile file) {
        this.files.add(file);
    }
}
