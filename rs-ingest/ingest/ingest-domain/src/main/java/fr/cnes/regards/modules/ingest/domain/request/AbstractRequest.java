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

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Basic request properties
 *
 * @author Marc SORDI
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_request",
    indexes = {
        @Index(name = "idx_request_search", columnList = "session_owner,session_name,provider_id"),
        @Index(name = "idx_request_remote_step_group_ids", columnList = "remote_step_group_ids")
})
@DiscriminatorColumn(name = "dtype", length = AbstractRequest.MAX_TYPE_LENGTH)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public abstract class AbstractRequest {

    public static final int MAX_TYPE_LENGTH = 32;

    @Id
    @SequenceGenerator(name = "requestSequence", initialValue = 1, sequenceName = "seq_request")
    @GeneratedValue(generator = "requestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(columnDefinition = "jsonb", name = "errors")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> errors;

    @ManyToOne
    @JoinColumn(name = "job_info_id", foreignKey = @ForeignKey(name = "fk_req_job_info_id"))
    private JobInfo jobInfo;

    @NotNull(message = "Creation date is required")
    @Column(name = "creation_date", nullable = false)
    private OffsetDateTime creationDate;

    /**
     * Remote request group id
     */
    @Column(columnDefinition = "jsonb", name = "remote_step_group_ids")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private List<String> remoteStepGroupIds;

    /**
     * Remote step dead line <br/>
     * A daemon controls this and passes this request in {@link InternalRequestState#ERROR} if deadline is outdated!
     */
    @Column(name = "remote_step_deadline")
    private OffsetDateTime remoteStepDeadline;


    @Column(length = 128, name = "session_owner")
    private String sessionOwner;

    @Column(length = 128, name = "session_name")
    private String session;

    @Column(length = 128, name = "provider_id")
    private String providerId;

    @NotNull(message = "Request state is required")
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private InternalRequestState state;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(length = 128, name = "dtype", insertable = false, updatable = false)
    private String dtype;

    public List<String> getRemoteStepGroupIds() {
        return remoteStepGroupIds;
    }

    public void setRemoteStepGroupIds(List<String> remoteStepGroupIds) {
        this.remoteStepGroupIds = remoteStepGroupIds;
    }

    public OffsetDateTime getRemoteStepDeadline() {
        return remoteStepDeadline;
    }

    public void setRemoteStepDeadline(OffsetDateTime remoteStepDeadline) {
        this.remoteStepDeadline = remoteStepDeadline;
    }

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

    public String getSessionOwner() {
        return sessionOwner;
    }

    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public InternalRequestState getState() {
        return state;
    }

    public void setState(InternalRequestState state) {
        this.state = state;
    }

    public String getDtype() {
        return dtype;
    }

    public void setDtype(String dtype) {
        this.dtype = dtype;
    }
}