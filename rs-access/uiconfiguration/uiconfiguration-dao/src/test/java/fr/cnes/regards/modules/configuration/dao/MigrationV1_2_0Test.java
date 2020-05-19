package fr.cnes.regards.modules.configuration.dao;

import org.junit.Test;
import scripts.uiconfiguration.V1_2_0__update_modules_conf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;

public class MigrationV1_2_0Test {

    @Test
    public void testSearchGraphUpdate() throws IOException {
        testUpdate("search-graph", V1_2_0__update_modules_conf::updateSearchGraphConfiguration);
    }

    @Test
    public void testSearchResultsUpdate() throws IOException {
        testUpdate("search-results", V1_2_0__update_modules_conf::updateSearchResultsConfiguration);
    }

    @Test
    public void testSearchFormUpdate() throws IOException {
        testUpdate("search-form", V1_2_0__update_modules_conf::updateSearchFormConfiguration);
    }

    public void testUpdate(String moduleType, Function<Map<String, Object>, Map<String, Object>> updater) throws IOException {
        String filePath = getClass().getClassLoader().getResource("fr.cnes.regards.modules.configuration.dao/" + moduleType + "-v1.1.0.conf.json").getPath();
        String oldConfiguration = new String(Files.readAllBytes(Paths.get(filePath)));
        String newConfiguration = V1_2_0__update_modules_conf.withParsedMap(oldConfiguration, updater);

        // For debug purposes
        // File oldFile = new File(filePath);
        // File testFolder = oldFile.getParentFile();
        // File newFile = new File(testFolder, "search-" + moduleType + "-v1.2.0.conf.json");
        // if (!newFile.exists()) {
        //     if (!newFile.createNewFile()) {
        //         Assert.fail("Could not create the file: " + newFile);
        //     }
        // }
        // Files.write(Paths.get(newFile.getAbsolutePath()), newConfiguration.getBytes());
    }
}
