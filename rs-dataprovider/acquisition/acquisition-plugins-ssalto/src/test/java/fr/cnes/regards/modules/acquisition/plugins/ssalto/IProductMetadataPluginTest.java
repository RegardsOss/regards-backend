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

import fr.cnes.regards.modules.acquisition.domain.plugins.IGenerateSIPPlugin;

public interface IProductMetadataPluginTest {

    /**
     * @return le chemin vers le fichier de proprietes du projet.
     */
    public String getProjectProperties();

    /**
     * Permet d'initialiser la liste des tests en utilisant les methodes <code>addPluginTestDef</code>
     */
    public void initTestList();

    /**
     * Permet d'initialiser le test unique en utilisant les methodes <code>addPluginTestDef</code>
     */
    public void initTestSoloList();

    /**
     * Permet d'initialiser le plugin a tester.
     * 
     * @return
     */
    public IGenerateSIPPlugin buildPlugin();
}
