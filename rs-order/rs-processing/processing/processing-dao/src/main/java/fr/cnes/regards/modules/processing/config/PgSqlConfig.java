package fr.cnes.regards.modules.processing.config;

import lombok.Value;

@Value
public class PgSqlConfig {
    String host;
    int port;
    String dbname;
    String schema;
    String user;
    String password;

}
