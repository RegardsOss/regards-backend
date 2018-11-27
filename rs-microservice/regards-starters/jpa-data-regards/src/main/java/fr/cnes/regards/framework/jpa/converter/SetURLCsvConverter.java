package fr.cnes.regards.framework.jpa.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.utils.RsRuntimeException;

/**
 * Converter for {@link Set} of {@link URL}, based on {@link SetStringCsvConverter}.
 * @author Sylvain VISSIERE-GUERINET
 */
@Converter
public class SetURLCsvConverter implements AttributeConverter<Set<URL>, String> {

    private static final SetStringCsvConverter stringConverter = new SetStringCsvConverter();

    private static final Logger LOG = LoggerFactory.getLogger(SetURLCsvConverter.class);

    @Override
    public String convertToDatabaseColumn(Set<URL> urls) {
        if (urls.isEmpty()) {
            return null;
        }
        return stringConverter
                .convertToDatabaseColumn(urls.stream().map(url -> url.toExternalForm()).collect(Collectors.toSet()));
    }

    @Override
    public Set<URL> convertToEntityAttribute(String dbData) {
        Set<URL> fromDb = new HashSet<>();
        for (String url : stringConverter.convertToEntityAttribute(dbData)) {
            try {
                fromDb.add(new URL(url));
            } catch (MalformedURLException e) {
                LOG.error(String.format("There was an issue when trying to recover an url from the data base: %s",
                                        e.getMessage()), e);
                throw new RsRuntimeException(e); // anyway it should never happens
            }
        }
        return fromDb;
    }
}
