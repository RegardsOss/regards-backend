/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.step.PStepFinal;
import fr.cnes.regards.modules.processing.domain.step.PStepIntermediary;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.api.Randomizer;
/**
 * This class allows to generate random instances for {@link PStep}
 *
 * @author gandrieu
 */
@AutoService(TypedRandomizer.class)
public class PStepTypedRandomizer implements TypedRandomizer<PStep> {

    @Override
    public Class<PStep> type() {
        return PStep.class;
    }

    @Override
    public Randomizer<PStep> randomizer(EasyRandom generator) {
        return () -> generator.nextBoolean() ? generator.nextObject(PStepFinal.class)
                : generator.nextObject(PStepIntermediary.class);
    }
}
