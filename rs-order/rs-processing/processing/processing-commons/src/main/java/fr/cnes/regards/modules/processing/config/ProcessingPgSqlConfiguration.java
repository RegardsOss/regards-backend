package fr.cnes.regards.modules.processing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessingPgSqlConfiguration {

    @Value("${regards.processing.r2dbc.host}")
    private String r2dbcHost;
    @Value("${regards.processing.r2dbc.port}")
    private Integer r2dbcPort;
    @Value("${regards.processing.r2dbc.username}")
    private String r2dbcUsername;
    @Value("${regards.processing.r2dbc.password}")
    private String r2dbcPassword;
    @Value("${regards.processing.r2dbc.dbname}")
    private String r2dbcDbname;
    @Value("${regards.processing.r2dbc.schema}")
    private String r2dbcSchema;

    @Bean
    public PgSqlProperties r2dbcPgSqlConfig() {
        return new PgSqlProperties(
                r2dbcHost,
                r2dbcPort,
                r2dbcDbname,
                r2dbcSchema,
                r2dbcUsername,
                r2dbcPassword
        );
    }

}
