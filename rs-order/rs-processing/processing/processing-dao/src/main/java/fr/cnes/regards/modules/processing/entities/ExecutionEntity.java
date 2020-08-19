package fr.cnes.regards.modules.processing.entities;

import fr.cnes.regards.modules.processing.domain.PStepSequence;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data @With
@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor

@Table("t_execution")
public class ExecutionEntity implements Persistable<UUID> {

    @Id @NonNull
    private UUID id;

    @Column("batch_id") @NonNull
    private UUID batchId;

    @Column("fileParameters") @NonNull
    private FileParameters fileParameters;

    @Column("timeoutAfterMillis") @NonNull
    private Long timeoutAfterMillis;

    @Column("steps") @NonNull
    private Steps steps;

    @Column("currentStatus")
    private ExecutionStatus currentStatus;

    @Column("tenant") @NonNull
    private String tenant;

    @Column("userName") @NonNull
    private String userName;

    @Column("process") @NonNull
    private String processName;

    @Column("created")
    @CreatedDate
    private OffsetDateTime created;

    @Column("lastUpdated")
    @LastModifiedDate
    private OffsetDateTime lastUpdated;

    @Version
    private Integer version;

    /**
     * Because the R2DBC driver has no post-load / post-persist hook for the moment,
     * this property is dealt with manually in the domain repository implementation.
     */
    @EqualsAndHashCode.Exclude
    @Transient
    private boolean persisted;

    public ExecutionEntity persisted() {
        this.persisted = true;
        return this;
    }

    @Override public boolean isNew() {
        return !persisted;
    }

}
