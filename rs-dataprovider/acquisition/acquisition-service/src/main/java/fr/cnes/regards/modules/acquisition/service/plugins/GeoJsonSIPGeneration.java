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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * This plugin allows to generate SIP by reading it into the content of the file to acquire.
 * @author Sébastien Binda
 */
@Plugin(id = "ReadSIPFromFile", version = "1.0.0-SNAPSHOT",
        description = "Read the SIP to generate from the file content.", author = "REGARDS Team",
        contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss")
public class GeoJsonSIPGeneration implements ISipGenerationPlugin {

    @Autowired
    private Gson gson;

    @Override
    public SIP generate(Product product) throws ModuleException {
        if (product.getActiveAcquisitionFiles().size() != 1) {
            throw new ModuleException("Each product should have only one json file");
        }
        // we cannot get NoSuchElementException here because we know there is exactly one AcquisitionFile link to this Product
        AcquisitionFile file = product.getAcquisitionFiles().iterator().next();
        Path sipFile = file.getFilePath();
        try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(sipFile.toFile())))) {
            return gson.fromJson(reader, SIP.class);
        } catch (IOException e) {
            throw new ModuleException(e);
        }
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

}
