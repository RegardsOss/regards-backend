/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.domain.config;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * Worker configuration
 *
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_worker_conf",
       uniqueConstraints = { @UniqueConstraint(name = "uk_worker_conf_worker_type",
                                               columnNames = { WorkerConfig.WORKER_TYPE_COLUMN_NAME }) })
public class WorkerConfig {

    public static final String WORKER_TYPE_COLUMN_NAME = "worker_type";

    public static final String CONTENT_TYPE_IN_NAME = "content_type_in";

    public static final String CONTENT_TYPE_OUT_NAME = "content_type_out";

    public static final String TABLE_CONTENT_TYPE_NAME = "ta_worker_conf_content_types_in";

    /**
     * List of Content Types treatable by this worker
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = CONTENT_TYPE_IN_NAME)
    @CollectionTable(name = TABLE_CONTENT_TYPE_NAME,
                     joinColumns = @JoinColumn(name = "worker_conf_id",
                                               foreignKey = @ForeignKey(name = "fk_worker_conf_content_type")),
                     uniqueConstraints = @UniqueConstraint(name = "uk_worker_conf_content_type",
                                                           columnNames = { CONTENT_TYPE_IN_NAME }))
    private final Set<String> contentTypeInputs = Sets.newHashSet();

    @Id
    @SequenceGenerator(name = "workerConfSequence", initialValue = 1, sequenceName = "seq_worker_conf")
    @GeneratedValue(generator = "workerConfSequence", strategy = GenerationType.SEQUENCE)
    @ConfigIgnore
    private Long id;

    @Column(length = 128, name = WORKER_TYPE_COLUMN_NAME)
    @NotNull
    private String workerType;

    @Column(name = CONTENT_TYPE_OUT_NAME)
    @Nullable
    private String contentTypeOutput;

    public static WorkerConfig build(String type, Set<String> contentTypesIn, String contentTypeOut) {
        WorkerConfig workerConfig = new WorkerConfig();
        workerConfig.workerType = type;
        workerConfig.contentTypeInputs.addAll(contentTypesIn);
        workerConfig.contentTypeOutput = contentTypeOut;
        return workerConfig;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWorkerType() {
        return workerType;
    }

    public void setWorkerType(String type) {
        this.workerType = type;
    }

    public Set<String> getContentTypeInputs() {
        return contentTypeInputs;
    }

    public void setContentTypeInputs(Set<String> contentTypeInputs) {
        this.contentTypeInputs.clear();
        this.contentTypeInputs.addAll(contentTypeInputs);
    }

    @Nullable
    public String getContentTypeOutput() {
        return contentTypeOutput;
    }

    public void setContentTypeOutput(@Nullable String contentTypeOutput) {
        this.contentTypeOutput = contentTypeOutput;
    }

    public WorkerConfigDto toDto() {
        return new WorkerConfigDto(this.workerType, this.contentTypeInputs, this.contentTypeOutput);
    }
}
