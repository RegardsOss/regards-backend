/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * Worker configuration
 *
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_worker_conf", uniqueConstraints = {
        @UniqueConstraint(name = "uk_worker_conf_type", columnNames = { WorkerConfig.TYPE_COLUMN_NAME }) })
public class WorkerConfig {

    public static final String TYPE_COLUMN_NAME = "type";

    public static final String CONTENT_TYPE_NAME = "content_type";

    /**
     * List of Content Types treatable by this worker
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = CONTENT_TYPE_NAME)
    @CollectionTable(name = "ta_worker_conf_content_types", joinColumns = @JoinColumn(name = "worker_conf_id", foreignKey = @ForeignKey(name = "fk_worker_conf_content_type")),
            uniqueConstraints = @UniqueConstraint(name = "uk_worker_conf_content_type", columnNames = { CONTENT_TYPE_NAME }))
    private final Set<String> contentTypes = Sets.newHashSet();

    @Id
    @SequenceGenerator(name = "storageLocationConfSequence", initialValue = 1,
            sequenceName = "seq_storage_location_conf")
    @GeneratedValue(generator = "storageLocationConfSequence", strategy = GenerationType.SEQUENCE)
    @ConfigIgnore
    private Long id;

    /**
     * Worker type
     */
    @Column(length = 128)
    @NotNull
    private String type;

    public static WorkerConfig build(String type, Set<String> contentTypes) {
        WorkerConfig workerConfig = new WorkerConfig();
        workerConfig.type = type;
        workerConfig.contentTypes.addAll(contentTypes);
        return workerConfig;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<String> getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(Set<String> contentTypes) {
        this.contentTypes.clear();
        this.contentTypes.addAll(contentTypes);
    }

    public WorkerConfigDto toDto() {
        return new WorkerConfigDto(this.type, this.contentTypes);
    }
}
