package fr.cnes.regards.modules.configuration.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

/**
*
* Class Configuration
*
* Configuration for projects IHMs
*
* @author Kevin Marchois
*/
@Entity
@Table(name = "t_ui_configuration", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ui_configuration_application_id", columnNames = { "application_id" }) })
public class UIConfiguration {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmConfigurationSequence", initialValue = 1, sequenceName = "seq_ui_configuration")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmConfigurationSequence")
    private Long id;

    /**
     * JSON representation of a configuration
     */
    @NotNull
    @Column(nullable = false)
    @Type(type = "text")
    private String configuration;

    @NotNull
    @Column(name = "application_id", nullable = false, length = 16)
    private String applicationId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((applicationId == null) ? 0 : applicationId.hashCode());
        result = (prime * result) + ((configuration == null) ? 0 : configuration.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UIConfiguration other = (UIConfiguration) obj;
        if (applicationId == null) {
            if (other.applicationId != null) {
                return false;
            }
        } else if (!applicationId.equals(other.applicationId)) {
            return false;
        }
        if (configuration == null) {
            if (other.configuration != null) {
                return false;
            }
        } else if (!configuration.equals(other.configuration)) {
            return false;
        }
        return true;
    }

}
