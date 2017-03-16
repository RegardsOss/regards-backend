package fr.cnes.regards.modules.search.rest;

import org.springframework.core.convert.converter.Converter;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class StringToResultClassEnumConverter<T extends Enum> implements Converter<String, T> {

    private final Class<T> enumType;

    public StringToResultClassEnumConverter(Class<T> enumType) {
        this.enumType = enumType;
    }

    @Override
    public T convert(String source) {
        if (source.length() == 0) {
            // It's an empty enum identifier: reset the enum value to null.
            return null;
        }
        return (T) Enum.valueOf(this.enumType, source.trim());
    }
}