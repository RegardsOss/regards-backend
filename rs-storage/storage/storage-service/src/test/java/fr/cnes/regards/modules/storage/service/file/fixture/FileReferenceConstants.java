/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.modules.storage.service.file.fixture;

import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;

import java.util.Set;

/**
 * @author Olivier Navarro
 **/
public class FileReferenceConstants {

    public static final String CHECKSUM1 = RandomChecksumUtils.generateRandomChecksum();

    public static final String CHECKSUM2 = RandomChecksumUtils.generateRandomChecksum();

    public static final Set<String> CHECKSUMS = Set.of(CHECKSUM1, CHECKSUM2);

    public static final String URL1 = "https://myhost1.com/fileName.txt";

    public static final String URL2 = "https://myhost2.com/fileName.txt";

    public static final Set<String> URLS = Set.of(URL1, URL2);

    public static final String STORAGE1 = "storage1";

    public static final String STORAGE2 = "storage2";

    public static final Set<String> STORAGES = Set.of(STORAGE1, STORAGE2);

    public static final String OWNER1 = "owner1";

    public static final String OWNER2 = "owner2";

    public static final Set<String> OWNERS = Set.of(OWNER1, OWNER2);

    public static final String SESSION1_OWNER = "SESSION 1 OWNER";

    public static final String SESSION2_OWNER = "SESSION 2 OWNER";

    public static final String SESSION1 = "SESSION 1";

    public static final String SESSION2 = "SESSION 2";

    public static final String FILE_REF_NAME = "file.test";

    private FileReferenceConstants() {
    }
}
