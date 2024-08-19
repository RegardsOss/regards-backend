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
package fr.cnes.regards.modules.feature.dto.validation;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.feature.dto.FeatureFileAttributes;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.MimeType;

import java.util.Set;

/**
 * Test for {@link FeatureFileAttributesValidator}
 *
 * @author Thibaud Michaudel
 **/
public class FeatureFileAttributesValidationTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void valid_checksum() {
        Assertions.assertTrue(validateChecksum("c26f20f5ef4453400b47fc98c3ce86b3", "MD5").isEmpty(),
                              "There should be no validation error");
    }

    @Test
    public void valid_checksum_no_md5() {
        Assertions.assertTrue(validateChecksum("&é  '(-&é&'", "SHA-1").isEmpty(),
                              "There should be no validation error");
    }

    @Test
    public void invalid_checksum_too_short() {
        Assertions.assertFalse(validateChecksum("c26f20f5ef445340", "MD5").isEmpty(),
                               "There should be a validation error");
    }

    @Test
    public void invalid_checksum_too_long() {
        Assertions.assertFalse(validateChecksum("c26f20f5ef4453400b47fc98c3ce86b3c26f20f5ef4453400b47fc98c3ce86b3",
                                                "MD5").isEmpty(), "There should be a validation error");
    }

    @Test
    public void invalid_checksum_empty() {
        Assertions.assertFalse(validateChecksum("", "MD5").isEmpty(), "There should be a validation error");
    }

    @Test
    public void invalid_checksum_null() {
        Assertions.assertFalse(validateChecksum(null, "MD5").isEmpty(), "There should be a validation error");
        Assertions.assertFalse(validateChecksum("c26f20f5ef4453400b47fc98c3ce86b3", null).isEmpty(),
                               "There should be a validation error");
    }

    @Test
    public void invalid_checksum_forbidden_characters() {
        Assertions.assertFalse(validateChecksum(" 26f20f5ef4453400b47fc98c3ce86b3", "MD5").isEmpty(),
                               "There should be a validation error");
        Assertions.assertFalse(validateChecksum("c26f20f5ef445340&b47fc98c3ce86b3", "MD5").isEmpty(),
                               "There should be a validation error");
        Assertions.assertFalse(validateChecksum("c26f20f5ef4453403b47fc98c3ce86bw", "MD5").isEmpty(),
                               "There should be a validation error");
    }

    private Set<ConstraintViolation<FeatureFileAttributes>> validateChecksum(String checksum, String algorithm) {
        FeatureFileAttributes featureFileAttributes = FeatureFileAttributes.build(DataType.OTHER,
                                                                                  MimeType.valueOf("text/plain"),
                                                                                  "fileName",
                                                                                  0L,
                                                                                  algorithm,
                                                                                  checksum);
        return validator.validate(featureFileAttributes);

    }
}
