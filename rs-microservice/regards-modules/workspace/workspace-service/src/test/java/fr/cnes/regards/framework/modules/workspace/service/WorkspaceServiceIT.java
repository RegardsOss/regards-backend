package fr.cnes.regards.framework.modules.workspace.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(properties = { "regards.cipher.key-location=src/test/resources/testKey",
        "regards.cipher.iv=1234567812345678", "spring.datasource.type=" })
public class WorkspaceServiceIT extends AbstractRegardsServiceIT {

    @Autowired
    private IWorkspaceService workspaceService;

    @Test
    public void testSetIntoWorkspace() throws IOException {
        Path src = Paths.get("src", "test", "resources", "test.txt");
        InputStream is = Files.newInputStream(src);
        workspaceService.setIntoWorkspace(is, "test.txt");
        Path pathInWS = Paths.get(workspaceService.getMicroserviceWorkspace().toString(), "test.txt");
        Assert.assertTrue(Files.exists(pathInWS));
    }

    @Test
    public void testRetrieveFromWS() throws IOException, NoSuchAlgorithmException {
        Path src = Paths.get("src", "test", "resources", "test.txt");
        InputStream is = Files.newInputStream(src);
        workspaceService.setIntoWorkspace(is, "test.txt");
        InputStream result = workspaceService.retrieveFromWorkspace("test.txt");
        // lets check the checksum so we are sure it is the same than original
        String srcChecksum = ChecksumUtils.computeHexChecksum(Files.newInputStream(src), "MD5");
        String wsChecksum = ChecksumUtils.computeHexChecksum(result, "MD5");
        Assert.assertEquals(srcChecksum, wsChecksum);
    }

}
