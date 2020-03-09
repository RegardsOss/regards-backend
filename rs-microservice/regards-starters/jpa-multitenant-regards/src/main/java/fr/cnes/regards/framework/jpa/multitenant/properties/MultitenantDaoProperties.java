/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.jpa.multitenant.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import fr.cnes.regards.framework.jpa.utils.MigrationTool;

/**
 * POJO for microservice configuration
 * @author Sébastien Binda
 */
@ConfigurationProperties("regards.jpa.multitenant")
public class MultitenantDaoProperties {

    /**
     * Multitenant transaction manager
     */
    public static final String MULTITENANT_TRANSACTION_MANAGER = "multitenantsJpaTransactionManager";

    /**
     * Does the dao is enabled ?
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * Projects configurations
     */
    private List<TenantConnection> tenants = new ArrayList<>();

    /**
     * Does the Multitenant dao is embedded ?
     */
    private Boolean embedded = Boolean.FALSE;

    /**
     * Path for embedded databases
     */
    private String embeddedPath;

    /**
     * Global hibernate dialect for all tenants
     */
    private String dialect = "fr.cnes.regards.framework.jpa.utils.CustomPostgresDialect";

    /**
     * For pooled data source, min available connections
     */
    private Integer minPoolSize = 3;

    /**
     * For pooled data source, max available connections
     */
    private Integer maxPoolSize = 5;

    /**
     * Default test query
     */
    private String preferredTestQuery = "SELECT 1";

    /**
     * Migration tool. Default to hbm2ddl.
     */
    private MigrationTool migrationTool = MigrationTool.FLYWAYDB;

    /**
     * Optional script output file for {@link MigrationTool#HBM2DDL}
     */
    private String outputFile = null;

    public List<TenantConnection> getTenants() {
        return tenants;
    }

    public void setTenants(final List<TenantConnection> pPTenants) {
        tenants = pPTenants;
    }

    public Boolean getEmbedded() {
        return embedded;
    }

    public void setEmbedded(final Boolean pEmbedded) {
        embedded = pEmbedded;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(final String pDriverClassName) {
        dialect = pDriverClassName;
    }

    public String getEmbeddedPath() {
        return embeddedPath;
    }

    public void setEmbeddedPath(final String pEmbeddedPath) {
        embeddedPath = pEmbeddedPath;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final Boolean pEnabled) {
        enabled = pEnabled;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(Integer pMinPoolSize) {
        minPoolSize = pMinPoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer pMaxPoolSize) {
        maxPoolSize = pMaxPoolSize;
    }

    public String getPreferredTestQuery() {
        return preferredTestQuery;
    }

    public void setPreferredTestQuery(String pPreferredTestQuery) {
        preferredTestQuery = pPreferredTestQuery;
    }

    public MigrationTool getMigrationTool() {
        return migrationTool;
    }

    public void setMigrationTool(MigrationTool pMigrationTool) {
        migrationTool = pMigrationTool;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String pOutputFile) {
        outputFile = pOutputFile;
    }

}
