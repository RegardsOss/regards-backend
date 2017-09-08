/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class Jason2DorisPluginTest extends Jason2PluginTest {

    @Override
    public void initTestList() {
        // TODO CMZ : corriger failed tests
//        addPluginTestDef("DA_TC_JASON2_DORIS10_FLAG", "JASON2/DOR10_INVALIDES",
//                         "DJ2_FLA_1PaP20090117_113657_20090117_000010_20090117_102444");
//        addPluginTestDef("DA_TC_JASON2_DORIS10_FLAG", "JASON2/DOR10_INVALIDES", "DJ2_FLA_1PaD20081203_000051");
//        addPluginTestDef("DA_TC_JASON2_DORIS10_COM", "JASON2/COMMERCIALES_10",
//                         "DJ2_MEC_1PaP20081226_113759_20081225_235951_20081226_100205");
//        addPluginTestDef("DA_TC_JASON2_DORIS10_COM", "JASON2/COMMERCIALES_10", "DJ2_MEC_1PaD20081202_235951");
//        addPluginTestDef("DA_TC_JASON2_DORIS10_PUB", "JASON2/PUBLIQUES_10", "DJ2_MEP_1PaD20081015_235951");
//        addPluginTestDef("DA_TC_JASON2_DORIS10_PUB", "JASON2/PUBLIQUES_10",
//                         "DJ2_MEP_1PaP20081017_113759_20081016_235951_20081017_102515");
    }

    @Override
    public void initTestSoloList() {
//        addPluginTestDef("DA_TC_JASON2_DORIS10_FLAG", "JASON2/DOR10_INVALIDES",
//                         "DJ2_FLA_1PaP20090117_113657_20090117_000010_20090117_102444");
    }

}
