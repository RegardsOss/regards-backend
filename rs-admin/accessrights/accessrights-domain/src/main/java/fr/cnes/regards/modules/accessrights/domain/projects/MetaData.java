/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import fr.cnes.regards.modules.accessrights.domain.UserVisibility;

/*
 * LICENSE_PLACEHOLDER
 */
@Entity(name = "T_META_DATA")
@SequenceGenerator(name = "metaDataSequence", initialValue = 1, sequenceName = "SEQ_META_DATA")
public class MetaData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "metaDataSequence")
    @Column(name = "id")
    private Long id;

    @Column(name = "key", unique = true)
    private String key;

    @Column(name = "value")
    private String value;

    @Column(name = "visibility")
    private UserVisibility visibility;

    public MetaData() {
        super();
    }

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

}
