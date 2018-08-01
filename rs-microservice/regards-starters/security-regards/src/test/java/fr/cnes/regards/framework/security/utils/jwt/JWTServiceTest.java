/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.utils.jwt;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

/**
 * @author msordi
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JwtTestConfiguration.class })
public class JWTServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTServiceTest.class);

    private static final String TENANT = "tenant";

    private static final String EMAIL = "marc.sordi@c-s.fr";

    private static final String ROLE = "USER";

    /**
     * JWT service
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Test JWT generation without group
     */
    @Test
    public void generateJWT() {

        // Generate token
        final String jwt = jwtService.generateToken(TENANT, EMAIL, ROLE);
        LOGGER.debug(jwt);

        // Parse token and retrieve user information
        try {
            final JWTAuthentication jwtAuth = jwtService.parseToken(new JWTAuthentication(jwt));

            Assert.assertEquals(TENANT, jwtAuth.getTenant());

            final UserDetails user = jwtAuth.getPrincipal();
            Assert.assertEquals(EMAIL, user.getName());
            Assert.assertEquals(ROLE, user.getRole());
        } catch (JwtException e) {
            final String message = "Error while generating JWT without group";
            LOGGER.debug(message, e);
            Assert.fail(message);
        }
    }

    @Test
    public void generateUserSpecificToken() throws InterruptedException, InvalidJwtException {
        Map<String, Object> addParams = new HashMap<String, Object>() {{
            put("toto", "titi");
        }};
        String token = jwtService
                .generateToken(TENANT, EMAIL, ROLE, OffsetDateTime.now().plus(3, ChronoUnit.DAYS), addParams, "pouet",
                               true);

        try {
            jwtService.parseToken(token, "teuop");
            Assert.fail("An exception should have been thrown here caused to an invalid secret key");
        } catch (JwtException e) {
        }
        Claims claims = jwtService.parseToken(token, "pouet");
        Assert.assertNotNull(claims.get("toto"));

        String expiredToken = jwtService
                .generateToken(TENANT, EMAIL, ROLE, OffsetDateTime.now(), addParams, "pouet", false);
        Thread.sleep(1_000);
        try {
            claims = jwtService.parseToken(expiredToken, "pouet");
            Assert.fail("An exception should have been thrown here caused to an expired token");
        } catch (ExpiredJwtException e) {
        }
    }
}
