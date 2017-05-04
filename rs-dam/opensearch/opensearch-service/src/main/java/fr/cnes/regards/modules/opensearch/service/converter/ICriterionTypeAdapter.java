package fr.cnes.regards.modules.opensearch.service.converter;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterBean;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.opensearch.service.queryparser.QueryParser;

/**
 * Type adapter reading an OpenSearch query string and converting it to an {@link ICriterion}
 * @author Xavier-Alexandre Brochard
 */
@GsonTypeAdapterBean(type = ICriterion.class)
public class ICriterionTypeAdapter extends TypeAdapter<ICriterion> {

    /**
     * Regards Query Parser. Autowired by Spring. Must not be null.
     */
    private final QueryParser queryParser;

    /**
     * Gson. Autowired by Spring. Must not be null.
     */
    private final Gson gson;

    /**
     * @param pQueryParser Regards Query Parser. Autowired by Spring. Must not be null.
     * @param pGson Gson. Autowired by Spring. Must not be null.
     */
    public ICriterionTypeAdapter(QueryParser pQueryParser, Gson pGson) {
        super();
        queryParser = pQueryParser;
        gson = pGson;
    }

    @Override
    public ICriterion read(JsonReader in) throws IOException {
        try {
            String query = in.nextString();
            return queryParser.parse(query);
        } catch (OpenSearchParseException e) {
            throw new JsonSyntaxException("An error occured during the parsing of the OpenSearch query", e);
        }

    }

    /* (non-Javadoc)
     * @see com.google.gson.TypeAdapter#write(com.google.gson.stream.JsonWriter, java.lang.Object)
     */
    @Override
    public void write(JsonWriter pOut, ICriterion pValue) throws IOException {
        pOut.jsonValue(gson.toJson(pValue));
    }

}
