/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.utils.random;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.api.Randomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows to generate random instances for {@link POutputFile.Digest}
 *
 * @author gandrieu
 */
public class POutputFileDigestTypedRandomizer implements TypedRandomizer<POutputFile.Digest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(POutputFileDigestTypedRandomizer.class);

    @Override
    public Class<POutputFile.Digest> type() {
        return POutputFile.Digest.class;
    }

    @Override
    public Randomizer<POutputFile.Digest> randomizer(EasyRandom generator) {
        return () -> new POutputFile.Digest("SHA-256", createDigestValue(generator));
    }

    public String createDigestValue(EasyRandom generator) {
        try {
            return ByteSource.wrap(generator.nextObject(String.class).getBytes()).hash(Hashing.sha256()).toString();
        } catch (Exception e) {
            LOGGER.debug("Unexpected error while creating digest", e);
            return "wups";
        }
    }
}
