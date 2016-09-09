package fr.cnes.regards.modules.access.domain;

public class ConfigParameter implements IPair<String, String> {

    private final String name;

    private final String value;

    public ConfigParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.access.domain.IPair#getKey()
     */
    @Override
    public String getKey() {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.access.domain.IPair#getValue()
     */
    @Override
    public String getValue() {
        return value;
    }

}
