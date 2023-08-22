/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.domain.input;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;

import javax.persistence.*;
import java.util.Objects;

/**
 * Association between {@link DeliveryRequest} and {@link JobInfo}.
 *
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "ta_delivery_request_job_info")
public class DeliveryAndJob {

    @Id
    @SequenceGenerator(name = "deliveryAndJobSequence", sequenceName = "seq_delivery_and_job")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deliveryAndJobSequence")
    private Long id;

    @OneToOne
    @JoinColumn(name = "delivery_id")
    private DeliveryRequest deliveryRequest;

    @OneToOne
    @JoinColumn(name = "job_id")
    private JobInfo jobInfo;

    public DeliveryAndJob() {
        // no-args constructor for jpa
    }

    public DeliveryAndJob(DeliveryRequest deliveryRequest, JobInfo jobInfo) {
        this.deliveryRequest = deliveryRequest;
        this.jobInfo = jobInfo;
    }

    public Long getId() {
        return id;
    }

    public DeliveryRequest getDeliveryRequest() {
        return deliveryRequest;
    }

    public JobInfo getJobInfo() {
        return jobInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeliveryAndJob that = (DeliveryAndJob) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DeliveryAndJob{" + "id=" + id + ", deliveryRequest=" + deliveryRequest + ", jobInfo=" + jobInfo + '}';
    }
}
