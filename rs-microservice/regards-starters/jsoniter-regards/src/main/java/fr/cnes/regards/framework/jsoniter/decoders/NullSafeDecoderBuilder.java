package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.ValueType;
import com.jsoniter.spi.Decoder;

import java.io.IOException;

public interface NullSafeDecoderBuilder extends SmartDecoder {

    class NullSafeDecoder implements Decoder {

        private final Decoder wrapped;

        public NullSafeDecoder(Decoder wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Object decode(JsonIterator iter) throws IOException {
            ValueType valueType = iter.whatIsNext();
            if (valueType == ValueType.NULL) {
                iter.readNull();
                return null;
            } else {
                return wrapped.decode(iter);
            }
        }
    }

    default Decoder nullSafe() {
        return new NullSafeDecoder(this);
    }

}
