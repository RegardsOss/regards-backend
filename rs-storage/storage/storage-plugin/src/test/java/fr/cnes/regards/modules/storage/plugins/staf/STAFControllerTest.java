package fr.cnes.regards.modules.storage.plugins.staf;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.staf.STAFArchive;
import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.STAFConfiguration;
import fr.cnes.regards.framework.staf.STAFException;
import fr.cnes.regards.framework.staf.STAFManager;
import fr.cnes.regards.framework.staf.STAFService;
import fr.cnes.regards.modules.storage.plugin.staf.domain.STAFController;

public class STAFControllerTest {

    @Test
    public void testPrepareFilesToStore() throws IOException, STAFException {

        STAFConfiguration configuration = new STAFConfiguration();

        STAFArchive stafArchive = new STAFArchive();
        stafArchive.setArchiveName("ARCHIVE_TEST");

        STAFService stafService = new STAFService(STAFManager.getInstance(configuration), stafArchive);

        STAFController controller = new STAFController(configuration, Paths.get("/home/sbinda/test/workspace"),
                stafService);

        Map<String, Set<Path>> filesToArchive = Maps.newHashMap();

        filesToArchive.put("le/beau/noeud", Sets.newHashSet(Paths.get("/home/sbinda/test/income")));

        controller.prepareFilesToArchive(filesToArchive, STAFArchiveModeEnum.NORMAL);

    }

}
