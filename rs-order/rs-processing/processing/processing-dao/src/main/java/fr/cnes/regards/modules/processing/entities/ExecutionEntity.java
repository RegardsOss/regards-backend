package fr.cnes.regards.modules.processing.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

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

    /**
     * Because the R2DBC driver has no post-load / post-persist hook for the moment,
     * this property is dealt with manually in the domain repository implementation.
     */
    @EqualsAndHashCode.Exclude
    @Transient
    private boolean persisted;

    private ExecutionEntity persisted() {
        this.persisted = true;
        return this;
    }

    @Override public boolean isNew() {
        return !persisted;
    }
}
