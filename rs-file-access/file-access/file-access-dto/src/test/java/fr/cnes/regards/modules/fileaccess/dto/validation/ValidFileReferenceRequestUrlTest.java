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
package fr.cnes.regards.modules.fileaccess.dto.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test class to check url validator
 *
 * @author Sébastien Binda
 **/
class ValidFileReferenceRequestUrlTest {

    @ParameterizedTest
    @ValueSource(strings = { "http://my.domain.fr/directory/file.dat",
                             "http://my.domain.fr:123/directory/file.dat",
                             "https://my.domain.fr/directory/file.dat",
                             "https://my.domain.fr:8080/directory/file.dat",
                             "file://my.domain.fr/directory/file.dat",
                             "file:///my.domain.fr/directory/file.dat",
                             "file:/my.domain.fr/directory/file.dat",
                             "staf://project/node/file.tar?fileName=file.dat",
                             "staf://project/node/file.dat?parts=12",
                             "s3://bucket/directory/file.dat" })
    void test_valid_urls(String url) {
        FileReferenceRequestUrlValidator validator = new FileReferenceRequestUrlValidator();
        Assertions.assertTrue(validator.isValidUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = { "http://my.domain.fr/directory/file#.dat",
                             "http://my.domain#.fr/directory/file#.dat",
                             "https://my.domain.fr:8080/directory#/file.dat",
                             "/my/file.dat",
                             "file:file.dat",
                             "file:///dir/file?.dat",
                             "file:///dir/file#.dat",
                             "s3://bucket/file#.dat" })
    void test_invalid_urls(String url) {
        FileReferenceRequestUrlValidator validator = new FileReferenceRequestUrlValidator();
        Assertions.assertFalse(validator.isValidUrl(url));
    }

}
