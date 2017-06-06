/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;

/**
 * Domain class representing a project user's meta datum.
 *
 * @author CS
 */
@Entity
// user_id is the JoinColumn defined in ProjectUser
@Table(name = "t_metadata", uniqueConstraints = @UniqueConstraint(name = "uk_metadata_key_user_id", columnNames = { "key", "user_id" }))
@SequenceGenerator(name = "metaDataSequence", initialValue = 1, sequenceName = "seq_metadata")
public class MetaData implements IIdentifiable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "metaDataSequence")
    @Column(name = "id")
    private Long id;

    @Column(name = "key", length = 64)
    private String key;

    @Column(name = "value", length = 255)
    private String value;

    @Column(name = "visibility")
    @Enumerated(EnumType.STRING)
    private UserVisibility visibility;

    public MetaData() {
        super();
    }

    public MetaData(String key, String value, UserVisibility visibility) {
        this.key=key;
        this.value=value;
        this.visibility=visibility;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MetaData other = (MetaData) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MetaData [id=" + id + ", key=" + key + ", value=" + value + ", visibility=" + visibility + "]";
    }

}
