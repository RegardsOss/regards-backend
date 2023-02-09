package fr.cnes.regards.framework.s3.client;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import fr.cnes.regards.framework.s3.domain.StorageCommand;
import fr.cnes.regards.framework.s3.domain.StorageCommandID;
import fr.cnes.regards.framework.s3.domain.StorageConfig;
import fr.cnes.regards.framework.s3.domain.StorageEntry;
import io.vavr.Tuple;
import io.vavr.control.Option;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.ByteBuffer;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class S3HighLevelReactiveClientTest {

    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

    public static final String X_AMZ_BUCKET_REGION = "X-Amz-Bucket-Region";

    public static final String X_AMZ_REQUEST_ID = "X-Amz-Request-Id";

    public static final String ACCEPT_RANGES = "Accept-Ranges";

    public static final String CONTENT_LENGTH = "content-length";

    public static final String E_TAG = "ETag";

    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

    public static final String VARY = "Vary";

    private String s3Host = "http://localhost:8081";

    private String key = "key";

    private String secret = "secret";

    private String region = "region";

    private String bucket = "bucketest";

    private String rootPath = "some/root/path";

    @ClassRule
    public static WireMockClassRule classRule = new WireMockClassRule(options().port(8081));

    // Divide @ClassRule and @Rule,
    // to bypass JUnit limitation of "@Rule cannot be static"
    @Rule
    public WireMockClassRule rule = classRule;

    private StorageConfig config;

    private S3HighLevelReactiveClient client;

    private final ResponseDefinitionBuilder mockUploadPartKOResponse = aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

    private final ResponseDefinitionBuilder mockUploadPartOKResponse = aResponse().withStatus(HttpStatus.OK.value())
                                                                                  .withHeader(ACCEPT_RANGES, "bytes")
                                                                                  .withHeader(CONTENT_LENGTH, "0")
                                                                                  .withHeader(CONTENT_SECURITY_POLICY,
                                                                                              "block-all-mixed-content")
                                                                                  .withHeader(X_AMZ_BUCKET_REGION,
                                                                                              "fr-regards-1")
                                                                                  .withHeader(E_TAG,
                                                                                              "\"5f363e0e58a95f06cbe9bbc662c5dfb6\"")
                                                                                  .withHeader(STRICT_TRANSPORT_SECURITY,
                                                                                              "max-age=31536000; includeSubDomains")
                                                                                  .withHeader(VARY,
                                                                                              "Origin, Accept-Encoding")
                                                                                  .withHeader(X_AMZ_REQUEST_ID,
                                                                                              "174191761287707B");

    private final ResponseDefinitionBuilder mockUploadInitResponseOK = aResponse().withStatus(HttpStatus.OK.value())
                                                                                  .withHeader(ACCEPT_RANGES, "bytes")
                                                                                  .withHeader(CONTENT_LENGTH, "328")
                                                                                  .withHeader(CONTENT_SECURITY_POLICY,
                                                                                              "block-all-mixed-content")
                                                                                  .withHeader(X_AMZ_BUCKET_REGION,
                                                                                              "fr-regards-1")
                                                                                  .withHeader(VARY,
                                                                                              "Origin, Accept-Encoding")
                                                                                  .withHeader(X_AMZ_REQUEST_ID,
                                                                                              "174190A05F388595")
                                                                                  .withHeader(HttpHeaders.CONTENT_TYPE,
                                                                                              MediaType.APPLICATION_XML_VALUE)
                                                                                  .withBody(
                                                                                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                                                                      + "<InitiateMultipartUploadResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Bucket>bucketest</Bucket><Key>some/root/path/big.txt</Key><UploadId>YTZhYTY5YWEtNDRiNC00ODM5LWE4ZDItNThiYzkzN2IxYTE1LjE0ZDA3YzBkLWVkYTYtNGE1ZS05OWY4LTFhMzc5MmVhNTg1Mw</UploadId></InitiateMultipartUploadResult>");

    private final ResponseDefinitionBuilder mockUpdateCompleteResponseOK = aResponse().withStatus(HttpStatus.OK.value())
                                                                                      .withHeader(ACCEPT_RANGES,
                                                                                                  "bytes")
                                                                                      .withHeader(CONTENT_LENGTH, "291")
                                                                                      .withHeader(
                                                                                          CONTENT_SECURITY_POLICY,
                                                                                          "block-all-mixed-content")
                                                                                      .withHeader(X_AMZ_BUCKET_REGION,
                                                                                                  "fr-regards-1")
                                                                                      .withHeader(VARY,
                                                                                                  "Origin, Accept-Encoding")
                                                                                      .withHeader(X_AMZ_REQUEST_ID,
                                                                                                  "174190A05F388595")
                                                                                      .withHeader(HttpHeaders.CONTENT_TYPE,
                                                                                                  MediaType.APPLICATION_XML_VALUE)
                                                                                      .withBody(
                                                                                          "<CompleteMultipartUploadResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                                                                                          + "<Location>http://Example-Bucket.s3.amazonaws.com/Example-Object</Location>"
                                                                                          + "<Bucket>bucketest</Bucket>"
                                                                                          + "<Key>some/root/path/big.txt</Key>"
                                                                                          + "<ETag>3858f62230ac3c915f300c664312c11f-9</ETag>"
                                                                                          + "</CompleteMultipartUploadResult>");

    @Before
    public void init() {
        config = StorageConfig.builder(s3Host, region, key, secret)
                              .bucket(bucket)
                              .rootPath(rootPath)
                              .maxRetriesNumber(3)
                              .retryBackOffBaseDuration(1)
                              .retryBackOffMaxDuration(2)
                              .build();
        client = new S3HighLevelReactiveClient(Schedulers.immediate(), 5 * 1024 * 1024, 10);
    }

    @Test
    public void test_retry_part_upload_on_server_error() {

        // Start upload multipart
        givenThat(post("/bucketest/some/root/path/big.txt?uploads").willReturn(mockUploadInitResponseOK));
        // Send first part ok
        givenThat(put(urlPathEqualTo("/bucketest/some/root/path/big.txt")).inScenario("FIX_S3")
                                                                          .whenScenarioStateIs(Scenario.STARTED)
                                                                          .willReturn(mockUploadPartOKResponse)
                                                                          .willSetStateTo("FIRST_PART_DONE"));
        // Send second part failed
        givenThat(put(urlPathEqualTo("/bucketest/some/root/path/big.txt")).inScenario("FIX_S3")
                                                                          .whenScenarioStateIs("FIRST_PART_DONE")
                                                                          .willReturn(mockUploadPartKOResponse)
                                                                          .willSetStateTo("ALMOST_WORKS"));
        // First retry second part failed
        givenThat(put(urlPathEqualTo("/bucketest/some/root/path/big.txt")).inScenario("FIX_S3")
                                                                          .whenScenarioStateIs("ALMOST_WORKS")
                                                                          .willReturn(mockUploadPartKOResponse)
                                                                          .willSetStateTo("ALMOST2_WORKS"));
        // Second retry second part failed
        givenThat(put(urlPathEqualTo("/bucketest/some/root/path/big.txt")).inScenario("FIX_S3")
                                                                          .whenScenarioStateIs("ALMOST2_WORKS")
                                                                          .willReturn(mockUploadPartKOResponse)
                                                                          .willSetStateTo("NOW_WORKS"));
        // Other part upload request success
        givenThat(put(urlPathEqualTo("/bucketest/some/root/path/big.txt")).inScenario("FIX_S3")
                                                                          .whenScenarioStateIs("NOW_WORKS")
                                                                          .willReturn(mockUploadPartOKResponse));
        // Send complete multipart ok
        givenThat(post(urlPathEqualTo("/bucketest/some/root/path/big.txt")).inScenario("FIX_S3")
                                                                           .whenScenarioStateIs("NOW_WORKS")

                                                                           .willReturn(mockUpdateCompleteResponseOK));
        // Init mocks
        getAllScenarios();

        // Send upload request
        long size = 10L * 1024L * 1024L + 512L;
        client.write(getStorageCommand(size)).block().matchWriteResult(success -> { // NOSONAR
            assertThat(success.getSize()).isEqualTo(size);
            return true;
        }, unreachableStorage -> {
            fail("s3 unreachable");
            return false;
        }, failure -> {
            fail(failure.toString());
            return false;
        });
    }

    private StorageCommand.Write getStorageCommand(long size) {
        Flux<ByteBuffer> buffers = Flux.just(ByteBuffer.wrap(new byte[(int) size]));
        StorageCommandID cmdId = new StorageCommandID("askId", UUID.randomUUID());
        String entryKey = config.entryKey("big.txt");
        StorageEntry entry = StorageEntry.builder()
                                         .checksum(Option.of(Tuple.of("MD5", "706126bf6d8553708227dba90694e81c")))
                                         .config(config)
                                         .size(Option.some(size))
                                         .fullPath(entryKey)
                                         .data(buffers)
                                         .build();
        return StorageCommand.write(config, cmdId, entryKey, entry);
    }

}
