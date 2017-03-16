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
@Table(name = "T_IHM_LAYOUTS")
public class Layout {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmLayoutsSequence", initialValue = 1, sequenceName = "SEQ_IHM_LAYOUTS")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmLayoutsSequence")
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true)
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

}
