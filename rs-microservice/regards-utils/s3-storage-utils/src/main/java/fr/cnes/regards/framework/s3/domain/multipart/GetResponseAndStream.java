package fr.cnes.regards.framework.s3.domain.multipart;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;

public class GetResponseAndStream implements AsyncResponseTransformer<GetObjectResponse, ResponseAndStream> {

    private static final Logger LOG = getLogger(GetResponseAndStream.class);

    private final CompletableFuture<ResponseAndStream> future = new CompletableFuture<>();

    private GetObjectResponse response;

    /**
     * The rate limit for the stream. rateLimit must be > 0.
     * The rate limit is used to limit the number of byte buffers prefetch in the reactive flux.
     * It is necessary to limit the rate, in case of buffers are not read at te same speed than
     * they are created.
     */
    private final int rateLimit;

    public GetResponseAndStream(int rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Override
    public CompletableFuture<ResponseAndStream> prepare() {
        return future;
    }

    @Override
    public void onResponse(GetObjectResponse response) {
        this.response = response;
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        Flux<ByteBuffer> bbFlux = Flux.from(preventClose(publisher))
                                      .limitRate(rateLimit)
                                      .doOnNext(bb -> LOG.debug("Read bytebuffer of {} size={}b",
                                                                response,
                                                                bb.remaining()));
        future.complete(new ResponseAndStream(response, bbFlux));
    }

    /*
     * For some reason, if we pass the publisher as is, it happens to be closed by netty
     * too quickly, and an exception occurs during an attempt to log.
     * So we add a useless operation on it to prevent this.
     */
    private Publisher<ByteBuffer> preventClose(SdkPublisher<ByteBuffer> publisher) {
        return publisher.filter(bb -> true);
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        future.completeExceptionally(error);
    }
}
