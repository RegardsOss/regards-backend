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
import fr.cnes.regards.framework.staf.protocol.STAFURLException;
import fr.cnes.regards.framework.staf.protocol.STAFURLFactory;
import fr.cnes.regards.framework.staf.protocol.STAFURLParameter;

public class STAFUrlFactoryTest {

    private final static String STAF_ARCHIVE = "ARCHIVE_TEST";

    private final static Path STAF_NODE = Paths.get("/node/test");

    private final static String STAF_FILE_NAME = "file.txt";

    private final static String STAF_TAR_NAME = "file.tar";

    private final static String STAF_CUT_NUMBER_OF_PARTS = "12";

    private final static String STAF_URL_NORMAL_TEST = String.format("%s://%s%s/%s", STAFURLFactory.STAF_URL_PROTOCOLE,
                                                                     STAF_ARCHIVE, STAF_NODE, STAF_FILE_NAME);

    private final static String STAF_URL_TAR_TEST = String
            .format("%s://%s%s/%s?%s=%s", STAFURLFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE, STAF_NODE, STAF_TAR_NAME,
                    STAFURLParameter.TAR_FILENAME_PARAMETER.getParameterName(), STAF_FILE_NAME);

    private final static String STAF_URL_CUT_TEST = String
            .format("%s://%s%s/%s?%s=%s", STAFURLFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE, STAF_NODE, STAF_FILE_NAME,
                    STAFURLParameter.CUT_PARTS_PARAMETER.getParameterName(), STAF_CUT_NUMBER_OF_PARTS);

    @BeforeClass
    public static void initAll() {
        STAFURLFactory.initSTAFURLProtocol();
    }

    @Test
    public void testExtractFromNormalFileInSTAF() throws MalformedURLException, STAFURLException {

        Assert.assertEquals("Error extracting STAF Archive name from STAF URL", STAF_ARCHIVE,
                            STAFURLFactory.getSTAFArchiveFromURL(new URL(STAF_URL_NORMAL_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_NODE,
                            STAFURLFactory.getSTAFNodeFromURL(new URL(STAF_URL_NORMAL_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_FILE_NAME,
                            STAFURLFactory.getSTAFFileNameFromURL(new URL(STAF_URL_NORMAL_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAFArchiveModeEnum.NORMAL,
                            STAFURLFactory.getSTAFArchiveModeFromURL(new URL(STAF_URL_NORMAL_TEST)));

    }

    @Test
    public void testExtractFromTARFileInSTAF() throws MalformedURLException, STAFURLException {

        Assert.assertEquals("Error extracting STAF Archive name from STAF URL", STAF_ARCHIVE,
                            STAFURLFactory.getSTAFArchiveFromURL(new URL(STAF_URL_TAR_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_NODE,
                            STAFURLFactory.getSTAFNodeFromURL(new URL(STAF_URL_TAR_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_TAR_NAME,
                            STAFURLFactory.getSTAFFileNameFromURL(new URL(STAF_URL_TAR_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAFArchiveModeEnum.TAR,
                            STAFURLFactory.getSTAFArchiveModeFromURL(new URL(STAF_URL_TAR_TEST)));

        Map<STAFURLParameter, String> paramers = STAFURLFactory.getSTAFURLParameters(new URL(STAF_URL_TAR_TEST));
        Assert.assertEquals("Error extracting STAF parameters from STAF URL", 1, paramers.size());

        Assert.assertEquals("Error extracting STAF parameters from STAF URL", STAF_FILE_NAME,
                            paramers.get(STAFURLParameter.TAR_FILENAME_PARAMETER));

    }

    @Test
    public void testExtractFromCutFileInSTAF() throws MalformedURLException, STAFURLException {

        Assert.assertEquals("Error extracting STAF Archive name from STAF URL", STAF_ARCHIVE,
                            STAFURLFactory.getSTAFArchiveFromURL(new URL(STAF_URL_CUT_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_NODE,
                            STAFURLFactory.getSTAFNodeFromURL(new URL(STAF_URL_CUT_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAF_FILE_NAME,
                            STAFURLFactory.getSTAFFileNameFromURL(new URL(STAF_URL_CUT_TEST)));

        Assert.assertEquals("Error extracting STAF Node from STAF URL", STAFArchiveModeEnum.CUT,
                            STAFURLFactory.getSTAFArchiveModeFromURL(new URL(STAF_URL_CUT_TEST)));

        Map<STAFURLParameter, String> paramers = STAFURLFactory.getSTAFURLParameters(new URL(STAF_URL_CUT_TEST));
        Assert.assertEquals("Error extracting STAF parameters from STAF URL", 1, paramers.size());

        Assert.assertEquals("Error extracting STAF parameters from STAF URL", STAF_CUT_NUMBER_OF_PARTS,
                            paramers.get(STAFURLParameter.CUT_PARTS_PARAMETER));

    }

    @Test(expected = STAFURLException.class)
    public void testInvalidURL() throws STAFURLException, MalformedURLException {
        STAFURLFactory.getSTAFArchiveFromURL(new URL("file:/test/node/file.txt"));
    }

    @Test(expected = STAFURLException.class)
    public void testInvalidURL2() throws STAFURLException, MalformedURLException {
        STAFURLFactory.getSTAFNodeFromURL(new URL("file:/test/node/file.txt"));
    }

    @Test(expected = STAFURLException.class)
    public void testInvalidURL3() throws STAFURLException, MalformedURLException {
        STAFURLFactory.getSTAFArchiveModeFromURL(new URL("file:/test/node/file.txt"));
    }

    @Test(expected = STAFURLException.class)
    public void testInvalidURL4() throws STAFURLException, MalformedURLException {
        STAFURLFactory.getSTAFFileNameFromURL(new URL("file:/test/node/file.txt"));
    }

}
