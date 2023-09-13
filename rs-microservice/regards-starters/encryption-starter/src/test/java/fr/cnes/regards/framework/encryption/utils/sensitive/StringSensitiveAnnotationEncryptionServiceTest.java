/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.encryption.utils.sensitive;

import fr.cnes.regards.framework.encryption.AESEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.encryption.sensitive.ISensitiveAnnotationEncryptionService;
import fr.cnes.regards.framework.encryption.sensitive.StringSensitiveAnnotationEncryptionService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

/**
 * The purpose of this test is to verify that {@link StringSensitiveAnnotationEncryptionService} encrypt, decrypt or
 * mask fields annotated with {@link fr.cnes.regards.framework.encryption.sensitive.StringSensitive} as expected.
 *
 * @author Iliana Ghazali
 **/
public class StringSensitiveAnnotationEncryptionServiceTest {

    private static final String MASK_PATTERN = "*******";

    private StringSensitiveAnnotationEncryptionService stringSensitiveService;

    @Before
    public void init() throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        AESEncryptionService aesEncryptionService = new AESEncryptionService();
        aesEncryptionService.init(new CipherProperties(Paths.get("src", "test", "resources", "testKey"),
                                                       "1234567812345678"));
        stringSensitiveService = new StringSensitiveAnnotationEncryptionService(aesEncryptionService);
    }

    @Test
    public void givenNotEncrypted_whenEncryptSensitive_thenEncrypted() {
        // GIVEN
        Person person = new Person("Neil Armstrong",
                                   "1234",
                                   new Coordinates("its-neil@nasa.com",
                                                   new SecretLocation("MoonCity", "Sea of Tranquility")));

        // WHEN
        stringSensitiveService.encryptObjectWithSensitiveValues(person);

        // THEN
        Person expectedEncryptedPerson = new Person("Neil Armstrong",
                                                    "v7LIsTELkDzB2dWQD7aSKg==",
                                                    new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                                    new SecretLocation("MoonCity",
                                                                                       "z6rqV9RqukoSftlHtoRHmNR+dF5xystRJT2uA0LdSik=")));
        Assertions.assertThat(person).isEqualTo(expectedEncryptedPerson);
    }

    @Test
    public void givenEncrypted_whenDecryptSensitive_thenDecrypted() {
        // GIVEN
        Person person = new Person("Neil Armstrong",
                                   "v7LIsTELkDzB2dWQD7aSKg==",
                                   new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                   new SecretLocation("MoonCity",
                                                                      "z6rqV9RqukoSftlHtoRHmNR"
                                                                      + "+dF5xystRJT2uA0LdSik=")));

        // WHEN
        stringSensitiveService.decryptOrMaskObjectWithSensitiveValues(person, false);

        // THEN
        Person expectedDecryptedPerson = new Person("Neil Armstrong",
                                                    "1234",
                                                    new Coordinates("its-neil@nasa.com",
                                                                    new SecretLocation("MoonCity",
                                                                                       "Sea of Tranquility")));
        Assertions.assertThat(person).isEqualTo(expectedDecryptedPerson);
    }

    @Test
    public void givenEncrypted_whenMaskSensitive_thenMasked() {
        // GIVEN
        Person person = new Person("Neil Armstrong",
                                   "v7LIsTELkDzB2dWQD7aSKg==",
                                   new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                   new SecretLocation("MoonCity",
                                                                      "z6rqV9RqukoSftlHtoRHmNR"
                                                                      + "+dF5xystRJT2uA0LdSik=")));

        // WHEN
        stringSensitiveService.decryptOrMaskObjectWithSensitiveValues(person, true);

        // THEN
        Person expectedDecryptedPerson = new Person("Neil Armstrong",
                                                    MASK_PATTERN,
                                                    new Coordinates(MASK_PATTERN,
                                                                    new SecretLocation("MoonCity", MASK_PATTERN)));
        Assertions.assertThat(person).isEqualTo(expectedDecryptedPerson);
    }

    @Test
    public void givenAlreadyEncrypted_whenEncryptSensitive_thenDoNothing() {
        // GIVEN
        Person previousPerson = new Person("Neil Armstrong",
                                           "v7LIsTELkDzB2dWQD7aSKg==",
                                           new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                           new SecretLocation("MoonCity",
                                                                              "z6rqV9RqukoSftlHtoRHmNR"
                                                                              + "+dF5xystRJT2uA0LdSik=")));
        // just update a value that is not sensitive
        // encrypted fields should remain encrypted
        Person updatedPerson = new Person("Neil Alden Armstrong",
                                          "v7LIsTELkDzB2dWQD7aSKg==",
                                          new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                          new SecretLocation("MoonCity",
                                                                             "z6rqV9RqukoSftlHtoRHmNR"
                                                                             + "+dF5xystRJT2uA0LdSik=")));
        // WHEN
        stringSensitiveService.encryptObjectWithSensitiveValues(updatedPerson, previousPerson);

        // THEN
        Person expectedDecryptedPerson = new Person("Neil Alden Armstrong",
                                                    "v7LIsTELkDzB2dWQD7aSKg==",
                                                    new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                                    new SecretLocation("MoonCity",
                                                                                       "z6rqV9RqukoSftlHtoRHmNR"
                                                                                       + "+dF5xystRJT2uA0LdSik=")));
        Assertions.assertThat(updatedPerson).isEqualTo(expectedDecryptedPerson);
    }

    @Test
    public void givenAlreadyEncrypted_whenPartialEncryptSensitive_thenEncryptToUpdate() {
        // GIVEN
        Person previousPerson = new Person("Neil Armstrong",
                                           "v7LIsTELkDzB2dWQD7aSKg==",
                                           new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                           new SecretLocation("MoonCity",
                                                                              "z6rqV9RqukoSftlHtoRHmNR"
                                                                              + "+dF5xystRJT2uA0LdSik=")));
        // update a value that is sensitive
        // field with new value should be encrypted, others should remain the same
        Person updatedPerson = new Person("Neil Armstrong",
                                          "new password",
                                          new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                          new SecretLocation("MoonCity",
                                                                             "z6rqV9RqukoSftlHtoRHmNR"
                                                                             + "+dF5xystRJT2uA0LdSik=")));
        // WHEN
        stringSensitiveService.encryptObjectWithSensitiveValues(updatedPerson, previousPerson);

        // THEN
        Person expectedDecryptedPerson = new Person("Neil Armstrong",
                                                    "uMwJ1W8b1WLvHf5UY1foAw==",
                                                    new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                                    new SecretLocation("MoonCity",
                                                                                       "z6rqV9RqukoSftlHtoRHmNR"
                                                                                       + "+dF5xystRJT2uA0LdSik=")));
        Assertions.assertThat(updatedPerson).isEqualTo(expectedDecryptedPerson);
    }

    @Test
    public void givenAlreadyMasked_whenEncryptSensitive_thenUpdateWithAlreadyEncrypted() {
        // GIVEN
        Person previousPerson = new Person("Neil Armstrong",
                                           "v7LIsTELkDzB2dWQD7aSKg==",
                                           new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                           new SecretLocation("MoonCity",
                                                                              "z6rqV9RqukoSftlHtoRHmNR"
                                                                              + "+dF5xystRJT2uA0LdSik=")));

        // just update a value that is not sensitive
        // encrypted fields should remain encrypted
        Person updatedPerson = new Person("Neil Alden Armstrong",
                                          ISensitiveAnnotationEncryptionService.MASK_PATTERN,
                                          new Coordinates(ISensitiveAnnotationEncryptionService.MASK_PATTERN,
                                                          new SecretLocation("MoonCity",
                                                                             ISensitiveAnnotationEncryptionService.MASK_PATTERN)));

        // WHEN
        stringSensitiveService.encryptObjectWithSensitiveValues(updatedPerson, previousPerson);

        // THEN
        Person expectedDecryptedPerson = new Person("Neil Alden Armstrong",
                                                    "v7LIsTELkDzB2dWQD7aSKg==",
                                                    new Coordinates("aSSOtmx/V/we7DSFL7xeQPSrWnckzsQf7a3ERqjnCtQ=",
                                                                    new SecretLocation("MoonCity",
                                                                                       "z6rqV9RqukoSftlHtoRHmNR"
                                                                                       + "+dF5xystRJT2uA0LdSik=")));
        Assertions.assertThat(updatedPerson).isEqualTo(expectedDecryptedPerson);
    }
}
