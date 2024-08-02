/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 * This class is the database entity corresponding to {@link fr.cnes.regards.modules.processing.domain.PBatch}
 *
 * @author gandrieu
 */
@Data
@With
@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor

@Table("t_batch")
public class BatchEntity implements Persistable<UUID> {

    @Id
    @NonNull
    private UUID id;

    @Column("process_business_id")
    @NonNull
    private UUID processBusinessId;

    @Column("correlation_id")
    @NonNull
    private String correlationId;

    @Column("tenant")
    @NonNull
    private String tenant;

    @Column("user_email")
    @NonNull
    private String userEmail;

    @Column("user_role")
    @NonNull
    private String userRole;

    @Column("parameters")
    @NonNull
    private ParamValues parameters;

    @Column("filesets")
    @NonNull
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

    @Override
    public boolean isNew() {
        return !persisted;
    }
}
