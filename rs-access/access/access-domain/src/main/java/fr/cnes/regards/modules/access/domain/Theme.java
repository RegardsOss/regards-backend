/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 *
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "T_NAVCTX_THEME")
@SequenceGenerator(name = "navCtxThemeSequence", initialValue = 1, sequenceName = "SEQ_NAVCTX_THEME")
public class Theme {

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "navCtxThemeSequence")
    private Long id;

    /**
     * A list of {@link ConfigParameter}
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "TA_THEME_PARAM",
            joinColumns = { @JoinColumn(name = "THEME_ID", referencedColumnName = "id",
                    foreignKey = @javax.persistence.ForeignKey(name = "FK_THEME_PARAM_ID")) },
            inverseJoinColumns = { @JoinColumn(name = "PARAMETER_ID", referencedColumnName = "id",
                    foreignKey = @javax.persistence.ForeignKey(name = "FK_PARAM_THEME_ID")) })
    private List<ConfigParameter> configuration;

    /**
     * This {@link Theme} the is a default {@link Theme}
     */
    private Boolean isDefault;

    /**
     * A type for the theme. A value of {@link ThemeType}.
     */
    private String themeType;

    /**
     * Default constructor
     */
    public Theme() {
        super();
    }

    /**
     * A constructor using fields.
     * 
     * @param pConfiguration
     *            a list of {@link ConfigParameter}
     * @param pDefault
     *            this is a default theme
     * @param pThemeType
     *            the {@link ThemeType} of the theme
     */
    public Theme(List<ConfigParameter> pConfiguration, Boolean pDefault, ThemeType pThemeType) {
        super();
        configuration = pConfiguration;
        isDefault = pDefault;
        themeType = pThemeType.toString();
    }

    public Long getId() {
        return id;
    }

    public List<ConfigParameter> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(List<ConfigParameter> pConfiguration) {
        configuration = pConfiguration;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean pIsDefault) {
        isDefault = pIsDefault;
    }

    public ThemeType getThemeType() {
        return ThemeType.valueOf(themeType);
    }

    public void setThemeType(ThemeType pThemeType) {
        themeType = pThemeType.toString();
    }

}
