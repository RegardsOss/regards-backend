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
package fr.cnes.regards.modules.order.service.utils;

import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * This class contains the data necessary to determine if a suborder gets too big or not.
 *
 * @author Guillaume Andrieu
 *
 */
@Component
public class SuborderSizeCounter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuborderSizeCounter.class);

    private static final int MAX_EXTERNAL_BUCKET_FILE_COUNT = 1_000;

    @Value("${regards.order.files.bucket.size.Mb:100}")
    private int storageBucketSizeMb;

    // Storage bucket size in bytes
    private Long storageBucketSize;

    /**
     * Method called at creation AND after a resfresh
     */
    @PostConstruct
    public void init() {
        // Compute storageBucketSize from storageBucketSizeMb filled by Spring
        storageBucketSize = storageBucketSizeMb * 1024L * 1024L;
        LOGGER.info("SuborderSizeCounter created/refreshed with, storageBucketSize={}", storageBucketSize);
    }

    public Long getStorageBucketSize() {
        return storageBucketSize;
    }

    public boolean storageBucketTooBig(Set<OrderDataFile> storageBucketFiles) {
        return storageBucketFiles.stream().mapToLong(DataFile::getFilesize).sum() >= storageBucketSize;
    }

    public boolean externalBucketTooBig(Set<OrderDataFile> externalBucketFiles) {
        return externalBucketFiles.size() > MAX_EXTERNAL_BUCKET_FILE_COUNT;
    }

    public long maxExternalBucketSize() {
        return MAX_EXTERNAL_BUCKET_FILE_COUNT;
    }
}
