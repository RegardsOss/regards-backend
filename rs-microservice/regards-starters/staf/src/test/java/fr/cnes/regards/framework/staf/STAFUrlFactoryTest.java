/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.regards.framework.staf.domain.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.protocol.STAFUrlException;
import fr.cnes.regards.framework.staf.protocol.STAFUrlFactory;
import fr.cnes.regards.framework.staf.protocol.STAFUrlParameter;

public class STAFUrlFactoryTest {

    private final static String STAF_ARCHIVE = "ARCHIVE_TEST";

    private final static Path STAF_NODE = Paths.get("/node/test");

    private final static String STAF_FILE_NAME = "file.txt";

    private final static String STAF_TAR_NAME = "file.tar";

    private final static String STAF_CUT_NUMBER_OF_PARTS = "12";

    private final static String STAF_URL_NORMAL_TEST = String.format("%s://%s%s/%s", STAFUrlFactory.STAF_URL_PROTOCOLE,
                                                                     STAF_ARCHIVE, STAF_NODE, STAF_FILE_NAME);

    private final static String STAF_URL_TAR_TEST = String
            .format("%s://%s%s/%s?%s=%s", STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE, STAF_NODE, STAF_TAR_NAME,
                    STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName(), STAF_FILE_NAME);

    private final static String STAF_URL_CUT_TEST = String
            .format("%s://%s%s/%s?%s=%s", STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE, STAF_NODE, STAF_FILE_NAME,
                    STAFUrlParameter.CUT_PARTS_PARAMETER.getParameterName(), STAF_CUT_NUMBER_OF_PARTS);

    @BeforeClass
    public static void initAll() {
        STAFUrlFactory.initSTAFURLProtocol();
    }

    @Test
    public void testExtractFromNormalFileInSTAF() throws MalformedURLException, STAFUrlException {

        Assert.assertEquals("Error extracting STAF Archive name from STAF URL", STAF_ARCHIVE,
                            STAFUrlFactory.getSTAFArchiveFromURL(new URL(STAF_URL_NORMAL_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_NODE,
                            STAFUrlFactory.getSTAFNodeFromURL(new URL(STAF_URL_NORMAL_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_FILE_NAME,
                            STAFUrlFactory.getSTAFFileNameFromURL(new URL(STAF_URL_NORMAL_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAFArchiveModeEnum.NORMAL,
                            STAFUrlFactory.getSTAFArchiveModeFromURL(new URL(STAF_URL_NORMAL_TEST)));

    }

    @Test
    public void testExtractFromTARFileInSTAF() throws MalformedURLException, STAFUrlException {

        Assert.assertEquals("Error extracting STAF Archive name from STAF URL", STAF_ARCHIVE,
                            STAFUrlFactory.getSTAFArchiveFromURL(new URL(STAF_URL_TAR_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_NODE,
                            STAFUrlFactory.getSTAFNodeFromURL(new URL(STAF_URL_TAR_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_TAR_NAME,
                            STAFUrlFactory.getSTAFFileNameFromURL(new URL(STAF_URL_TAR_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAFArchiveModeEnum.TAR,
                            STAFUrlFactory.getSTAFArchiveModeFromURL(new URL(STAF_URL_TAR_TEST)));

        Map<STAFUrlParameter, String> paramers = STAFUrlFactory.getSTAFURLParameters(new URL(STAF_URL_TAR_TEST));
        Assert.assertEquals("Error extracting STAF parameters from STAF URL", 1, paramers.size());

        Assert.assertEquals("Error extracting STAF parameters from STAF URL", STAF_FILE_NAME,
                            paramers.get(STAFUrlParameter.TAR_FILENAME_PARAMETER));

    }

    @Test
    public void testExtractFromCutFileInSTAF() throws MalformedURLException, STAFUrlException {

        Assert.assertEquals("Error extracting STAF Archive name from STAF URL", STAF_ARCHIVE,
                            STAFUrlFactory.getSTAFArchiveFromURL(new URL(STAF_URL_CUT_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_NODE,
                            STAFUrlFactory.getSTAFNodeFromURL(new URL(STAF_URL_CUT_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_FILE_NAME,
                            STAFUrlFactory.getSTAFFileNameFromURL(new URL(STAF_URL_CUT_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAFArchiveModeEnum.CUT,
                            STAFUrlFactory.getSTAFArchiveModeFromURL(new URL(STAF_URL_CUT_TEST)));

        Map<STAFUrlParameter, String> paramers = STAFUrlFactory.getSTAFURLParameters(new URL(STAF_URL_CUT_TEST));
        Assert.assertEquals("Error extracting STAF parameters from STAF URL", 1, paramers.size());

        Assert.assertEquals("Error extracting STAF parameters from STAF URL", STAF_CUT_NUMBER_OF_PARTS,
                            paramers.get(STAFUrlParameter.CUT_PARTS_PARAMETER));

    }

    @Test(expected = STAFUrlException.class)
    public void testInvalidURL() throws STAFUrlException, MalformedURLException {
        STAFUrlFactory.getSTAFArchiveFromURL(new URL("file:/test/node/file.txt"));
    }

    @Test(expected = STAFUrlException.class)
    public void testInvalidURL2() throws STAFUrlException, MalformedURLException {
        STAFUrlFactory.getSTAFNodeFromURL(new URL("file:/test/node/file.txt"));
    }

    @Test(expected = STAFUrlException.class)
    public void testInvalidURL3() throws STAFUrlException, MalformedURLException {
        STAFUrlFactory.getSTAFArchiveModeFromURL(new URL("file:/test/node/file.txt"));
    }

    @Test(expected = STAFUrlException.class)
    public void testInvalidURL4() throws STAFUrlException, MalformedURLException {
        STAFUrlFactory.getSTAFFileNameFromURL(new URL("file:/test/node/file.txt"));
    }

}
