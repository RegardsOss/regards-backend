/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.request;

import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

/**
 * Common request properties
 *
 * @author Marc SORDI
 *
 */
@MappedSuperclass
public abstract class AbstractRequest {

    @Column(columnDefinition = "jsonb", name = "errors")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> errors;

    @OneToOne
    @JoinColumn(name = "job_info_id", foreignKey = @ForeignKey(name = "fk_req_job_info_id"))
    private JobInfo jobInfo;

    @NotNull(message = "Creation date is required")
    @Column(name = "creation_date", nullable = false)
    private OffsetDateTime creationDate;

    public Set<String> getErrors() {
        return errors;
    }

    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        if (errors == null) {
            errors = new HashSet<>();
        }
        errors.add(error);
    }
    public JobInfo getJobInfo() {
        return jobInfo;
    }

    public void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
