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
 * Class Theme
 *
 * Entity to describe an IHM Theme configuration.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table(name = "T_IHM_THEMES")
public class Theme {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmThemesSequence", initialValue = 1, sequenceName = "SEQ_IHM_THEMES")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmThemesSequence")
    private Long id;

    /**
     * Theme name. Use to instantiate the right module
     */
    @NotNull
    @Column(nullable = false)
    private String name;

    /**
     * Active theme is the default theme used for IHM
     */
    @NotNull
    @Column(nullable = false)
    private boolean active;

    /**
     * Theme configuration
     */
    @NotNull
    @Column(nullable = false)
    @Type(type = "text")
    private String configuration;

    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String pName) {
        name = pName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean pActive) {
        active = pActive;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final String pConfiguration) {
        configuration = pConfiguration;
    }

}
