/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;

/**
 * Ingest configuration properties
 *
 * @author Marc SORDI
 *
 * FIXME faire un set du AIP download template à partir du controller donnant accès aux AIPs
 */
@Configuration
public class IngestConfigurationProperties {

    @Value("${regards.ingest.max.bulk.size:1000}")
    private Integer maxBulkSize;

    /**
     * See {@link IBatchHandler#getReceiveTimeout} for more information
     */
    @Value("${regards.ingest.batch.messages.timeout:1000}")
    private Long batchReceiveTimeout;

    /*
     * In minute.
     */
    @Value("${regards.ingest.remote.request.timeout:5}")
    private Long remoteRequestTimeout;

    public Integer getMaxBulkSize() {
        return maxBulkSize;
    }

    public void setMaxBulkSize(Integer maxBulkSize) {
        this.maxBulkSize = maxBulkSize;
    }

    public Long getRemoteRequestTimeout() {
        return remoteRequestTimeout;
    }

    public void setRemoteRequestTimeout(Long remoteRequestTimeout) {
        this.remoteRequestTimeout = remoteRequestTimeout;
    }

    public Long getBatchReceiveTimeout() {
        return batchReceiveTimeout;
    }

    public void setBatchReceiveTimeout(Long batchReceiveTimeout) {
        this.batchReceiveTimeout = batchReceiveTimeout;
    }

}
