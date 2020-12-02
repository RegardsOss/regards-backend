/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.auto.service.AutoService;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.api.Randomizer;

import java.io.IOException;

/**
 * This class allows to generate random instances for {@link POutputFile.Digest}
 *
 * @author gandrieu
 */
@AutoService(TypedRandomizer.class)
public class POutputFileDigestTypedRandomizer implements TypedRandomizer<POutputFile.Digest> {
    @Override public Class<POutputFile.Digest> type() {
        return POutputFile.Digest.class;
    }

    @Override public Randomizer<POutputFile.Digest> randomizer(EasyRandom generator) {
        return () -> new POutputFile.Digest("SHA-256", createDigestValue(generator));
    }

    public String createDigestValue(EasyRandom generator) {
        try {
            return ByteSource.wrap(generator.nextObject(String.class).getBytes()).hash(Hashing.sha256()).toString();
        }
        catch(Exception e) { return "wups"; }
    }
}
