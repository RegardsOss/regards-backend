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
package fr.cnes.regards.modules.storage.dao.entity.download;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

import java.util.Objects;

@Entity
@Table(name = "t_user_download_quota_counter",
       uniqueConstraints = @UniqueConstraint(name = UserDownloadQuotaEntity.UK_DOWNLOAD_QUOTA_COUNTER_INSTANCE_EMAIL,
                                             columnNames = { "instance_id", "email" }))
@SequenceGenerator(name = UserDownloadQuotaEntity.DOWNLOAD_QUOTA_SEQUENCE,
                   initialValue = 1,
                   sequenceName = "seq_download_quota_counter")
public class UserDownloadQuotaEntity {

    public static final String UK_DOWNLOAD_QUOTA_COUNTER_INSTANCE_EMAIL = "uk_download_quota_counter_instance_email";

    public static final String DOWNLOAD_QUOTA_SEQUENCE = "downloadQuotaSequence";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = DOWNLOAD_QUOTA_SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Column(name = "instance_id", nullable = false)
    private String instance;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "counter", nullable = false)
    @Min(value = -1, message = "The custom download quota cannot be inferior to -1 (minus one).")
    private Long counter;

    public UserDownloadQuotaEntity() {
        super();
    }

    public UserDownloadQuotaEntity(Long id, String instance, String email, Long counter) {
        this.instance = instance;
        this.id = id;
        this.email = email;
        this.counter = counter;
    }

    public UserDownloadQuotaEntity(String instance, String email, Long counter) {
        this.instance = instance;
        this.email = email;
        this.counter = counter;
    }

    public UserDownloadQuotaEntity(UserDownloadQuotaEntity other) {
        this.instance = other.instance;
        this.email = other.email;
        this.counter = other.counter;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getCounter() {
        return counter;
    }

    public void setCounter(Long counter) {
        this.counter = counter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDownloadQuotaEntity that = (UserDownloadQuotaEntity) o;
        return Objects.equals(id, that.id)
               && Objects.equals(instance, that.instance)
               && Objects.equals(email,
                                 that.email)
               && Objects.equals(counter, that.counter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, instance, email, counter);
    }
}
