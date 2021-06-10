package fr.cnes.regards.framework.modules.tenant.settings.domain;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DynamicTenantSettingDto<T> {
    private String name;
    private String description;
    private T value;
    private T defaultValue;

    public DynamicTenantSettingDto(DynamicTenantSetting setting) {
        this.name = setting.getName();
        this.description = setting.getDescription();
        this.value = setting.getValue();
        this.defaultValue = setting.getDefaultValue();
    }

    /**
     * Constructor for (de)serialization
     */
    public DynamicTenantSettingDto() {}

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DynamicTenantSettingDto<?> that = (DynamicTenantSettingDto<?>) o;

        return getName() != null ? getName().equals(that.getName()) : that.getName() == null;
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }
}
