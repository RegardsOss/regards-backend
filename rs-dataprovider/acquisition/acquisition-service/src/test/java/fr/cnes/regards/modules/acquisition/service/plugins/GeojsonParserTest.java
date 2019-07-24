/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.ingest.domain.SIP;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=geojson_test" })
public class GeojsonParserTest extends AbstractMultitenantServiceTest {

    @Autowired
    private Gson gson;

    @Test
    public void test() throws ModuleException, IOException {
        GeoJsonFeatureCollectionParserPlugin plugin = new GeoJsonFeatureCollectionParserPlugin();
        Path targetDir = Paths.get("target/output");
        if (Files.exists(targetDir)) {
            Files.walk(targetDir).map(Path::toFile).sorted((o1, o2) -> -o1.compareTo(o2)).forEach(File::delete);
            Files.deleteIfExists(targetDir);
        }
        Files.createDirectory(targetDir);
        Files.copy(Paths.get("src/test/resources/departements-version-simplifiee.geojson"),
                   Paths.get("target/output/departements-version-simplifiee.geojson"));
        Files.copy(Paths.get("src/test/resources/Ain.pdf"), Paths.get("target/output/Ain.pdf"));
        Files.copy(Paths.get("src/test/resources/Ain.png"), Paths.get("target/output/Ain.png"));
        Files.copy(Paths.get("src/test/resources/Ain.dat"), Paths.get("target/output/Ain.dat"));
        plugin.setDirectoryToScan(targetDir.toString());
        plugin.setGson(gson);
        plugin.setFeatureId("nom");
        List<Path> paths = plugin.scan(Optional.empty());
        Assert.assertEquals(1, paths.size());
    }

    @Test
    public void testSIPGenerationPlugin() throws ModuleException {
        GeoJsonSIPGeneration plugin = new GeoJsonSIPGeneration();
        plugin.setGson(gson);

        Product product = new Product();
        AcquisitionFile af = new AcquisitionFile();
        af.setFilePath(Paths.get("src/test/resources/Ain.json"));
        af.setState(AcquisitionFileState.ACQUIRED);
        product.addAcquisitionFile(af);

        SIP sip = plugin.generate(product);
        Assert.assertNotNull(sip);
        Assert.assertEquals("Ain", sip.getProperties().getDescriptiveInformation().get("nom"));
        Assert.assertNotNull(sip.getGeometry());
    }

}
