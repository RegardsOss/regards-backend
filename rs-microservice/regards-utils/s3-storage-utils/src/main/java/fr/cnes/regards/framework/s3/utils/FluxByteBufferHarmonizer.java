package fr.cnes.regards.framework.s3.utils;

import fr.cnes.regards.framework.s3.exception.S3ClientException;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static reactor.core.publisher.SignalType.*;

public class FluxByteBufferHarmonizer {

    private FluxByteBufferHarmonizer() {
    }

    /**
     * Transforms a flux of ByteBuffers having unknown sizes to a flux
     * of fixed-sized ByteBuffers. (The last one may differ...)
     *
     * @param fixedSize the size of each outputted byte buffer
     * @return an harmonious flux
     */
    public static Function<Flux<ByteBuffer>, Flux<ByteBuffer>> harmonize(int fixedSize, int prefetch) {
        return bbs -> {
            AtomicReference<ByteBuffer> fixedSizeBufferRef = new AtomicReference<>(ByteBuffer.allocate(fixedSize));
            return bbs.materialize()
                      .filter(s -> List.of(ON_NEXT, ON_COMPLETE, ON_ERROR).contains(s.getType()))
                      .concatMap(s -> switch (s.getType()) {
                          case ON_NEXT -> processNextByteBuffer(fixedSize, fixedSizeBufferRef, s);
                          case ON_COMPLETE -> processComplete(fixedSizeBufferRef.get());
                          case ON_ERROR -> Mono.error(Option.of(s.getThrowable())
                                                            .getOrElse(new S3ClientException("Unhanded error")));
                          default -> Mono.empty();
                      }, 0);
        };
    }

    public static Publisher<ByteBuffer> processComplete(ByteBuffer data) {
        if (data.position() > 0) {
            data.flip();
            return Mono.just(data);
        } else {
            return Mono.empty();
        }
    }

    public static Publisher<ByteBuffer> processNextByteBuffer(int fixedSize,
                                                              AtomicReference<ByteBuffer> fixedSizeBufferRef,
                                                              Signal<ByteBuffer> s) {
        final ByteBuffer nextBuffer = s.get();
        List<ByteBuffer> toFluxOut = List.empty();
        if (nextBuffer != null) {
            while (nextBuffer.hasRemaining()) {
                final ByteBuffer fillingUp = fixedSizeBufferRef.get();
                int loop = Math.min(fillingUp.remaining(), nextBuffer.remaining());
                for (int i = 0; i < loop; i++) {
                    fillingUp.put(nextBuffer.get());
                }
                if (!fillingUp.hasRemaining()) {
                    fillingUp.flip();
                    // Emitting the full fixed size buffer
                    toFluxOut = toFluxOut.append(fillingUp);
                    fixedSizeBufferRef.set(ByteBuffer.allocate(fixedSize));
                }
            }
        }
        return Flux.fromIterable(toFluxOut);
    }
}

