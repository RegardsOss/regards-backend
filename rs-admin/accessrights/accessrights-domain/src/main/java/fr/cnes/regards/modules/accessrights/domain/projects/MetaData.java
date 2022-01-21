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
package fr.cnes.regards.modules.accessrights.domain.projects;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * Domain class representing a project user's meta datum.
 *
 * @author CS
 */
@Entity
// user_id is the JoinColumn defined in ProjectUser
@Table(name = "t_metadata", uniqueConstraints = @UniqueConstraint(name = "uk_metadata_key_user_id", columnNames = {"key", "user_id"}))
@SequenceGenerator(name = "metaDataSequence", initialValue = 1, sequenceName = "seq_metadata")
public class MetaData implements IIdentifiable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "metaDataSequence")
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "key", length = 64)
    private String key;

    @Valid
    @Length(max = 255)
    @Column(name = "value", length = 255)
    private String value;

    @Column(name = "visibility")
    @Enumerated(EnumType.STRING)
    private UserVisibility visibility;

    public MetaData() {
    }

    /**
     * Constructor setting the parameters as attributes
     * @param key
     * @param value
     * @param visibility
     */
    public MetaData(String key, String value, UserVisibility visibility) {
        this.key = key;
        this.value = value;
        this.visibility = visibility;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String pKey) {
        key = pKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String pValue) {
        value = pValue;
    }

    public UserVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(final UserVisibility pVisibility) {
        visibility = pVisibility;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetaData)) return false;
        MetaData metaData = (MetaData) o;
        return Objects.equals(key, metaData.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "MetaData [id=" + id + ", key=" + key + ", value=" + value + ", visibility=" + visibility + "]";
    }

}
