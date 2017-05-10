/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

/**
 *
 * Class Layout
 *
 * Layout configuration for projects IHMs
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table(name = "t_ui_layout")
public class Layout {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmLayoutsSequence", initialValue = 1, sequenceName = "seq_ui_layout")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmLayoutsSequence")
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true, length = 16)
    private String applicationId;

    /**
     * JSON representation of layout configuration
     */
    @NotNull
    @Column(nullable = false)
    @Type(type = "text")
    protected String layout;

    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final String pApplicationId) {
        applicationId = pApplicationId;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(final String pLayout) {
        layout = pLayout;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((applicationId == null) ? 0 : applicationId.hashCode());
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
        final Layout other = (Layout) obj;
        if (applicationId == null) {
            if (other.applicationId != null) {
                return false;
            }
        } else
            if (!applicationId.equals(other.applicationId)) {
                return false;
            }
        return true;
    }

    @Override
    public String toString() {
        return "Layout [id=" + id + ", applicationId=" + applicationId + ", layout=" + layout + "]";
    }

}
