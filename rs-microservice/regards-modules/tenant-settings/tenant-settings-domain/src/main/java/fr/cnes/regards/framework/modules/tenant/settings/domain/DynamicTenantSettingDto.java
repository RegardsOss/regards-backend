package fr.cnes.regards.framework.modules.tenant.settings.domain;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DynamicTenantSettingDto<T> {

    private String name;

    private String description;

    private T defaultValue;

    private T value;

    public DynamicTenantSettingDto(String name, String description, T defaultValue, T value) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = value;
    }

    /**
     * Constructor for (de)serialization
     */
    public DynamicTenantSettingDto() {
    }

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
