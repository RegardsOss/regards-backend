package fr.cnes.regards.modules.access.domain;

import java.util.List;

/**
 *
 * @author christophe
 *
 */
public class Theme {

    private Long id_;

    private List<ConfigParameter> configuration_;

    private Boolean isDefault_;

    private ThemeType themeType_;

    public Long getId() {
        return id_;
    }

    public void setId(Long id) {
        id_ = id;
    }

    public List<ConfigParameter> getConfiguration() {
        return configuration_;
    }

    public void setConfiguration(List<ConfigParameter> configuration) {
        configuration_ = configuration;
    }

    public Boolean getIsDefault() {
        return isDefault_;
    }

    public void setIsDefault(Boolean isDefault) {
        isDefault_ = isDefault;
    }

    public ThemeType getThemeType() {
        return themeType_;
    }

    public void setThemeType(ThemeType themeType) {
        themeType_ = themeType;
    }

}
