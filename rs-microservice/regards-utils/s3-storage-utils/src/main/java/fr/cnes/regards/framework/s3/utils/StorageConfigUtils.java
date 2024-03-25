/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.s3.utils;

import fr.cnes.regards.framework.s3.dto.StorageConfigDto;
import io.vavr.control.Option;
import io.vavr.control.Try;
import software.amazon.awssdk.utils.StringUtils;

import java.net.URL;
import java.util.function.Function;

/**
 * Static methods for operations with {@link StorageConfigDto}
 *
 * @author Thibaud Michaudel
 **/
public class StorageConfigUtils {

    public static String entryKey(StorageConfigDto storageConfig, String suffix) {
        return normalizedRootPath(storageConfig) + suffix;
    }

    private static String normalizedRootPath(StorageConfigDto storageConfig) {
        return Option.of(storageConfig.getRootPath())
                     .filter(StringUtils::isNotBlank)
                     .map(s -> s.endsWith("/") ? s : s + "/")
                     .getOrElse("");
    }

    public static URL entryKeyUrl(StorageConfigDto storageConfigDto, String entryKey) {
        return Try.of(() -> new URL(String.format("%s/%s/%s",
                                                  storageConfigDto.getEndpoint(),
                                                  storageConfigDto.getBucket(),
                                                  entryKey)))
                  .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
    }
}
