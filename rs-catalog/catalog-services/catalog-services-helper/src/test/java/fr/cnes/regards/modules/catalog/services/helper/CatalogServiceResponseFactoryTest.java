package fr.cnes.regards.modules.catalog.services.helper;

import com.google.common.io.Files;
import com.google.common.net.HttpHeaders;
import fr.cnes.regards.modules.catalog.services.helper.CatalogPluginResponseFactory.CatalogPluginResponseType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class CatalogServiceResponseFactoryTest {

    public class TestObject {

        public final String value;

        public final Integer intValue;

        public TestObject(String value, Integer intValue) {
            super();
            this.value = value;
            this.intValue = intValue;
        }

    }

    @Test
    public void testXmlResponse() throws IOException {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ResponseEntity<StreamingResponseBody> result = CatalogPluginResponseFactory.createSuccessResponse(response,
                                                                                                          CatalogPluginResponseType.XML,
                                                                                                          new TestObject(
                                                                                                              "StringValue",
                                                                                                              10));
        Assert.assertEquals(MediaType.APPLICATION_XML_VALUE, result.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        validateTestResponse(result, new File("src/test/resources/result.xml"));
    }

    @Test
    public void testJsonResponse() throws IOException {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ResponseEntity<StreamingResponseBody> result = CatalogPluginResponseFactory.createSuccessResponse(response,
                                                                                                          CatalogPluginResponseType.JSON,
                                                                                                          new TestObject(
                                                                                                              "StringValue",
                                                                                                              10));
        Assert.assertEquals(MediaType.APPLICATION_JSON_VALUE, result.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        validateTestResponse(result, new File("src/test/resources/result.json"));
    }

    @Test
    public void testImageResponse() throws IOException {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ResponseEntity<StreamingResponseBody> result = CatalogPluginResponseFactory.createSuccessResponseFromFile(
            response,
            CatalogPluginResponseType.FILE_IMG_PNG,
            new File("src/test/resources/LogoCnes.png"));
        Assert.assertEquals(MediaType.IMAGE_PNG_VALUE, result.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        validateTestResponse(result, new File("src/test/resources/LogoCnes.png"));
    }

    @Test
    public void testDownloadResponse() throws IOException {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ResponseEntity<StreamingResponseBody> result = CatalogPluginResponseFactory.createSuccessResponseFromFile(
            response,
            CatalogPluginResponseType.FILE_DOWNLOAD,
            new File("src/test/resources/LogoCnes.png"));
        Assert.assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            result.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        validateTestResponse(result, new File("src/test/resources/LogoCnes.png"));
    }

    private void validateTestResponse(ResponseEntity<StreamingResponseBody> result, File expectedFileResult)
        throws IOException {

        Assert.assertEquals(HttpStatus.OK, result.getStatusCode());
        File resultFile = File.createTempFile("result", "");
        resultFile.deleteOnExit();
        StreamingResponseBody resultStream = result.getBody();
        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            resultStream.writeTo(fos);
            fos.close();
        }
        logFileContent(resultFile);
        logFileContent(expectedFileResult);
        Assert.assertTrue("Request result is not valid", Files.equal(expectedFileResult, resultFile));
    }

    private void logFileContent(File file) {
        try {
            FileInputStream fstream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            /* read log line by line */
            while ((strLine = br.readLine()) != null) {
                /* parse strLine to obtain what you want */
                System.out.println(strLine);
            }
            fstream.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

}
