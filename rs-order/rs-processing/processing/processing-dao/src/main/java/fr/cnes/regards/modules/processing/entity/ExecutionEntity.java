/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.entity;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * This class is the database entity corresponding to {@link fr.cnes.regards.modules.processing.domain.PExecution}
 *
 * @author gandrieu
 */
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

    @Column("file_parameters") @NonNull
    private FileParameters fileParameters;

    @Column("timeout_after_millis") @NonNull
    private Long timeoutAfterMillis;

    @Column("steps") @NonNull
    private Steps steps;

    @Column("current_status")
    private ExecutionStatus currentStatus;

    @Column("tenant") @NonNull
    private String tenant;

    @Column("user_email") @NonNull
    private String userEmail;

    @Column("process_business_id") @NonNull
    private UUID processBusinessId;

    @Column("correlation_id") @NonNull
    private String correlationId;

    @Column("batch_correlation_id") @NonNull
    private String batchCorrelationId;

    @Column("created")
    @CreatedDate
    private OffsetDateTime created;

    @Column("last_updated")
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
