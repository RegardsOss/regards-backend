package fr.cnes.regards.modules.processing.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.UUID;


@Data @With
@AllArgsConstructor
@RequiredArgsConstructor

@Table("t_outputfile")
public class OutputFileEntity implements Persistable<UUID> {

    private @Id UUID id;

    /** The execution this file has been generated for */
    private @Column("exec_id") UUID execId;

    /** Where to download from */
    private @Column("url") URL url;

    /** The file name */
    private @Column("name") String name;

    /** The file checksum */
    private @Column("checksum_value") String checksumValue;
    private @Column("checksum_method") String checksumMethod;

    /** The file size */
    private @Column("size_bytes") Long sizeInBytes;

    /** The file creation time (not the entity creation time) */
    private @Column("created") OffsetDateTime created;

    /** Whether the file has been downloaded or not */
    private @Column("downloaded") boolean downloaded;

    /** Whether the file has been deleted or not */
    private @Column("deleted") boolean deleted;

    /**
     * Because the R2DBC driver has no post-load / post-persist hook for the moment,
     * this property is dealt with manually in the domain repository implementation.
     */
    @EqualsAndHashCode.Exclude
    @Transient
    private boolean persisted;

    public OutputFileEntity persisted() {
        this.persisted = true;
        return this;
    }

    @Override public boolean isNew() {
        return !persisted;
    }
}
