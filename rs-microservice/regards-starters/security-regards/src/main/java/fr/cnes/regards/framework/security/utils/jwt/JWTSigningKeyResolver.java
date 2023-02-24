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
package fr.cnes.regards.framework.security.utils.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.lang.Assert;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * Resolve key according to passed JWT headers
 */
public class JWTSigningKeyResolver extends SigningKeyResolverAdapter {

    /**
     * Store key per signing algorithm
     */
    private final Map<SignatureAlgorithm, String> keyPerAlg;

    public JWTSigningKeyResolver(Map<SignatureAlgorithm, String> keyPerAlg) {
        this.keyPerAlg = keyPerAlg;
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        SignatureAlgorithm algorithm = SignatureAlgorithm.forName(header.getAlgorithm());
        return switch (algorithm) {
            case HS256, HS384, HS512 -> getSecretKeyFromString(getKey(algorithm), algorithm);
            case RS256, RS384, RS512 -> getPublicKeyFromString(getKey(algorithm));
            case ES256, ES384, ES512, PS256, PS384, PS512, NONE ->
                throw new UnsupportedOperationException(String.format("JWT signing algorithm not supported : %s",
                                                                      algorithm.getValue()));
        };
    }

    private String getKey(SignatureAlgorithm algorithm) {
        String key = keyPerAlg.get(algorithm);
        Assert.notNull(key, String.format("No key available for %s algorithm", algorithm.getValue()));
        return key;
    }

    private Key getSecretKeyFromString(String secretKey, SignatureAlgorithm algorithm) {
        return new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithm.getJcaName());
    }

    private PublicKey getPublicKeyFromString(String publicKey) {
        try {
            byte[] buffer = Base64.getDecoder().decode(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            return keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Error while resolving PublicKey for JWT parsing", e);
        }
    }

    /**
     * For testing purpose, allows injecting key programmatically
     */
    protected void putKey(SignatureAlgorithm algorithm, String key) {
        keyPerAlg.put(algorithm, key);
    }
}
