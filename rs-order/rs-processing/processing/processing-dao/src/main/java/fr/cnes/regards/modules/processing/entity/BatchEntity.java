package fr.cnes.regards.modules.processing.entity;

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

@Table("t_batch")
public class BatchEntity implements Persistable<UUID> {

    @Id @NonNull
    private UUID id;

    @Column("process_business_id") @NonNull
    private UUID processBusinessId;

    @Column("correlation_id") @NonNull
    private String correlationId;

    @Column("tenant") @NonNull
    private String tenant;

    @Column("user_email") @NonNull
    private String userEmail;

    @Column("user_role") @NonNull
    private String userRole;

    @Column("process_name") @NonNull
    private String processName;

    @Column("parameters") @NonNull
    private ParamValues parameters;

    @Column("filesets") @NonNull
    private FileStatsByDataset filesets;

    /**
     * Because the R2DBC driver has no post-load / post-persist hook for the moment,
     * this property is dealt with manually in the domain repository implementation.
     */
    @EqualsAndHashCode.Exclude
    @Transient
    private boolean persisted;

    public BatchEntity persisted() {
        this.persisted = true;
        return this;
    }

    @Override public boolean isNew() {
        return !persisted;
    }
}
