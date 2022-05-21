package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.geojson.geometry.*;
import io.vavr.collection.List;

import java.io.IOException;

public class IGeometryJsoniterDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new IGeometryJsoniterDecoder();
        JsoniterSpi.registerTypeDecoder(IGeometry.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any geometry = iter.readAny();
        if (isNull(geometry)) {
            return IGeometry.unlocated();
        }

        Any coordinates = geometry.get("coordinates");
        String type = geometry.toString("type").toUpperCase();
        switch (type) {
            case "POINT":
                return Point.fromArray(coordinates.as(double[].class));
            case "MULTIPOINT":
                return MultiPoint.fromArray(asDoubleArray2(coordinates));
            case "LINESTRING":
                return LineString.fromArray(asDoubleArray2(coordinates));
            case "MULTILINESTRING":
                return MultiLineString.fromArray(asDoubleArray3(coordinates));
            case "POLYGON":
                return Polygon.fromArray(asDoubleArray3(coordinates));
            case "MULTIPOLYGON":
                return MultiPolygon.fromArray(asDoubleArray4(coordinates));
            default:
                return null;
        }
    }

    private double[][][][] asDoubleArray4(Any coordinates) {
        return List.ofAll(coordinates.asList()).map(this::asDoubleArray3).toJavaArray(double[][][][]::new);
    }

    private double[][][] asDoubleArray3(Any coordinates) {
        return List.ofAll(coordinates.asList()).map(this::asDoubleArray2).toJavaArray(double[][][]::new);
    }

    private double[][] asDoubleArray2(Any coordinates) {
        return List.ofAll(coordinates.asList()).map(any -> any.as(double[].class)).toJavaArray(double[][]::new);
    }

}
