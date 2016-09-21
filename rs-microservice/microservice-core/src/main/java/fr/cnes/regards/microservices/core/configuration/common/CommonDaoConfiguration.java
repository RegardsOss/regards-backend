package fr.cnes.regards.microservices.core.configuration.common;

public class CommonDaoConfiguration {

    private String driverClassName;

    private String dialect;

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String pDriverClassName) {
        driverClassName = pDriverClassName;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String pDialect) {
        dialect = pDialect;
    }

}
