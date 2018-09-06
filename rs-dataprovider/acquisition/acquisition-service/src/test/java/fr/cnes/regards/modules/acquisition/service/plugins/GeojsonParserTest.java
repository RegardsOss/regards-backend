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
        plugin.setDirectoryToScan(targetDir.toString());
        plugin.setGson(gson);
        List<Path> paths = plugin.scan(Optional.empty());
        Assert.assertEquals(96, paths.size());
    }

    @Test
    public void testSIPGenerationPlugin() throws ModuleException {
        GeoJsonSIPGeneration plugin = new GeoJsonSIPGeneration();
        plugin.setGson(gson);

        Product product = new Product();
        AcquisitionFile af = new AcquisitionFile();
        af.setFilePath(Paths.get("src/test/resources/Ain.json"));
        product.addAcquisitionFile(af);
        SIP sip = plugin.generate(product);
        Assert.assertNotNull(sip);
        Assert.assertEquals("Ain", sip.getProperties().getDescriptiveInformation().get("nom"));
        Assert.assertNotNull(sip.getGeometry());
    }

}
