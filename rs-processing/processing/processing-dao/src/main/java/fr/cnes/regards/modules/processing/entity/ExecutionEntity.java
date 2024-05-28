/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * This class is the database entity corresponding to {@link fr.cnes.regards.modules.processing.domain.PExecution}
 *
 * @author gandrieu
 */
@Table("t_execution")
@SuppressWarnings("java:S1192") // Duplicated strings, but create constants make no sense here.
public class ExecutionEntity implements Persistable<UUID> {

    @Id
    @NotNull
    private UUID id;

    @Column("batch_id")
    @NotNull
    private UUID batchId;

    @Column("file_parameters")
    @NotNull
    private FileParameters fileParameters;

    @Column("timeout_after_millis")
    @NotNull
    private Long timeoutAfterMillis;

    @Column("steps")
    @NotNull
    private Steps steps;

    @Column("current_status")
    private ExecutionStatus currentStatus;

    @Column("tenant")
    @NotNull
    private String tenant;

    @Column("user_email")
    @NotNull
    private String userEmail;

    @Column("process_business_id")
    @NotNull
    private UUID processBusinessId;

    @Column("correlation_id")
    @NotNull
    private String correlationId;

    @Column("batch_correlation_id")
    @NotNull
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
    @Transient
    private boolean persisted;

    public ExecutionEntity() {
    }

    public ExecutionEntity(UUID id,
                           UUID batchId,
                           FileParameters fileParameters,
                           Long timeoutAfterMillis,
                           Steps steps,
                           @Nullable ExecutionStatus currentStatus,
                           String tenant,
                           String userEmail,
                           UUID processBusinessId,
                           String correlationId,
                           String batchCorrelationId,
                           @Nullable OffsetDateTime created,
                           @Nullable OffsetDateTime lastUpdated,
                           @Nullable Integer version,
                           boolean persisted) {
        Assert.notNull(id, "id is required");
        Assert.notNull(batchId, "batchId is required");
        Assert.notNull(fileParameters, "fileParameters is required");
        Assert.notNull(timeoutAfterMillis, "timeoutAfterMillis is required");
        Assert.notNull(steps, "steps is required");
        Assert.notNull(tenant, "tenant is required");
        Assert.notNull(userEmail, "userEmail is required");
        Assert.notNull(processBusinessId, "processBusinessId is required");
        Assert.notNull(correlationId, "correlationId is required");
        Assert.notNull(batchCorrelationId, "batchCorrelationId is required");
        this.id = id;
        this.batchId = batchId;
        this.fileParameters = fileParameters;
        this.timeoutAfterMillis = timeoutAfterMillis;
        this.steps = steps;
        this.currentStatus = currentStatus;
        this.tenant = tenant;
        this.userEmail = userEmail;
        this.processBusinessId = processBusinessId;
        this.correlationId = correlationId;
        this.batchCorrelationId = batchCorrelationId;
        this.created = created;
        this.lastUpdated = lastUpdated;
        this.version = version;
        this.persisted = persisted;
    }

    public ExecutionEntity(UUID id,
                           UUID batchId,
                           FileParameters fileParameters,
                           Long timeoutAfterMillis,
                           Steps steps,
                           String tenant,
                           String userEmail,
                           UUID processBusinessId,
                           String correlationId,
                           String batchCorrelationId) {
        this(id,
             batchId,
             fileParameters,
             timeoutAfterMillis,
             steps,
             null,
             tenant,
             userEmail,
             processBusinessId,
             correlationId,
             batchCorrelationId,
             null,
             null,
             null,
             false);
    }

    @Override
    public boolean isNew() {
        return !persisted;
    }

    public ExecutionEntity persisted() {
        this.persisted = true;
        return this;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        Assert.notNull(id, "id is required");
        this.id = id;
    }

    public ExecutionEntity withId(UUID id) {
        Assert.notNull(id, "id is required");
        this.id = id;
        return this;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        Assert.notNull(batchId, "batchId is required");
        this.batchId = batchId;
    }

    public ExecutionEntity withBatchId(UUID batchId) {
        Assert.notNull(batchId, "batchId is required");
        this.batchId = batchId;
        return this;
    }

    public FileParameters getFileParameters() {
        return fileParameters;
    }

    public void setFileParameters(FileParameters fileParameters) {
        Assert.notNull(fileParameters, "fileParameters is required");
        this.fileParameters = fileParameters;
    }

    public ExecutionEntity withFileParameters(FileParameters fileParameters) {
        Assert.notNull(fileParameters, "fileParameters is required");
        this.fileParameters = fileParameters;
        return this;
    }

    public Long getTimeoutAfterMillis() {
        return timeoutAfterMillis;
    }

    public void setTimeoutAfterMillis(Long timeoutAfterMillis) {
        Assert.notNull(timeoutAfterMillis, "timeoutAfterMillis is required");
        this.timeoutAfterMillis = timeoutAfterMillis;
    }

    public ExecutionEntity withTimeoutAfterMillis(Long timeoutAfterMillis) {
        Assert.notNull(timeoutAfterMillis, "timeoutAfterMillis is required");
        this.timeoutAfterMillis = timeoutAfterMillis;
        return this;
    }

    public Steps getSteps() {
        return steps;
    }

    public void setSteps(Steps steps) {
        Assert.notNull(steps, "steps is required");
        this.steps = steps;
    }

    public ExecutionEntity withSteps(Steps steps) {
        Assert.notNull(steps, "steps is required");
        this.steps = steps;
        return this;
    }

    public ExecutionStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ExecutionStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public ExecutionEntity withCurrentStatus(ExecutionStatus currentStatus) {
        this.currentStatus = currentStatus;
        return this;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        Assert.notNull(tenant, "tenant is required");
        this.tenant = tenant;
    }

    public ExecutionEntity withTenant(String tenant) {
        Assert.notNull(tenant, "tenant is required");
        this.tenant = tenant;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        Assert.notNull(userEmail, "userEmail is required");
        this.userEmail = userEmail;
    }

    public ExecutionEntity withUserEmail(String userEmail) {
        Assert.notNull(userEmail, "userEmail is required");
        this.userEmail = userEmail;
        return this;
    }

    public UUID getProcessBusinessId() {
        return processBusinessId;
    }

    public void setProcessBusinessId(UUID processBusinessId) {
        Assert.notNull(processBusinessId, "processBusinessId is required");
        this.processBusinessId = processBusinessId;
    }

    public ExecutionEntity withProcessBusinessId(UUID processBusinessId) {
        Assert.notNull(processBusinessId, "processBusinessId is required");
        this.processBusinessId = processBusinessId;
        return this;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        Assert.notNull(correlationId, "correlationId is required");
        this.correlationId = correlationId;
    }

    public ExecutionEntity withCorrelationId(String correlationId) {
        Assert.notNull(correlationId, "correlationId is required");
        this.correlationId = correlationId;
        return this;
    }

    public String getBatchCorrelationId() {
        return batchCorrelationId;
    }

    public void setBatchCorrelationId(String batchCorrelationId) {
        Assert.notNull(batchCorrelationId, "batchCorrelationId is required");
        this.batchCorrelationId = batchCorrelationId;
    }

    public ExecutionEntity withBatchCorrelationId(String batchCorrelationId) {
        Assert.notNull(batchCorrelationId, "batchCorrelationId is required");
        this.batchCorrelationId = batchCorrelationId;
        return this;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public ExecutionEntity withCreated(OffsetDateTime created) {
        this.created = created;
        return this;
    }

    public OffsetDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public ExecutionEntity withLastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public ExecutionEntity withVersion(Integer version) {
        this.version = version;
        return this;
    }

    public boolean isPersisted() {
        return persisted;
    }

    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    public ExecutionEntity withPersisted(boolean persisted) {
        this.persisted = persisted;
        return this;
    }

    public String toString() {
        return "ExecutionEntity(id="
               + this.getId()
               + ", batchId="
               + this.getBatchId()
               + ", fileParameters="
               + this.getFileParameters()
               + ", timeoutAfterMillis="
               + this.getTimeoutAfterMillis()
               + ", steps="
               + this.getSteps()
               + ", currentStatus="
               + this.getCurrentStatus()
               + ", tenant="
               + this.getTenant()
               + ", userEmail="
               + this.getUserEmail()
               + ", processBusinessId="
               + this.getProcessBusinessId()
               + ", correlationId="
               + this.getCorrelationId()
               + ", batchCorrelationId="
               + this.getBatchCorrelationId()
               + ", created="
               + this.getCreated()
               + ", lastUpdated="
               + this.getLastUpdated()
               + ", version="
               + this.getVersion()
               + ", persisted="
               + this.isPersisted()
               + ")";
    }

    public int hashCode() {
        int result = 1;
        result = result * 59 + (this.timeoutAfterMillis == null ? 43 : this.timeoutAfterMillis.hashCode());
        result = result * 59 + (this.getVersion() == null ? 43 : this.getVersion().hashCode());
        result = result * 59 + (this.getId() == null ? 43 : this.getId().hashCode());
        result = result * 59 + (this.getBatchId() == null ? 43 : this.getBatchId().hashCode());
        result = result * 59 + (this.getFileParameters() == null ? 43 : this.getFileParameters().hashCode());
        result = result * 59 + (this.getSteps() == null ? 43 : this.getSteps().hashCode());
        result = result * 59 + (this.getCurrentStatus() == null ? 43 : this.getCurrentStatus().hashCode());
        result = result * 59 + (this.getTenant() == null ? 43 : this.getTenant().hashCode());
        result = result * 59 + (this.getUserEmail() == null ? 43 : this.getUserEmail().hashCode());
        result = result * 59 + (this.getProcessBusinessId() == null ? 43 : this.getProcessBusinessId().hashCode());
        result = result * 59 + (this.getCorrelationId() == null ? 43 : this.getCorrelationId().hashCode());
        result = result * 59 + (this.getBatchCorrelationId() == null ? 43 : this.getBatchCorrelationId().hashCode());
        result = result * 59 + (this.getCreated() == null ? 43 : this.getCreated().hashCode());
        result = result * 59 + (this.getLastUpdated() == null ? 43 : this.getLastUpdated().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExecutionEntity that = (ExecutionEntity) o;

        if (persisted != that.persisted) {
            return false;
        }
        if (!id.equals(that.id)) {
            return false;
        }
        if (!batchId.equals(that.batchId)) {
            return false;
        }
        if (!fileParameters.equals(that.fileParameters)) {
            return false;
        }
        if (!timeoutAfterMillis.equals(that.timeoutAfterMillis)) {
            return false;
        }
        if (!steps.equals(that.steps)) {
            return false;
        }
        if (currentStatus != that.currentStatus) {
            return false;
        }
        if (!tenant.equals(that.tenant)) {
            return false;
        }
        if (!userEmail.equals(that.userEmail)) {
            return false;
        }
        if (!processBusinessId.equals(that.processBusinessId)) {
            return false;
        }
        if (!correlationId.equals(that.correlationId)) {
            return false;
        }
        if (!batchCorrelationId.equals(that.batchCorrelationId)) {
            return false;
        }
        if (!Objects.equals(created, that.created)) {
            return false;
        }
        if (!Objects.equals(lastUpdated, that.lastUpdated)) {
            return false;
        }
        return Objects.equals(version, that.version);
    }
}
