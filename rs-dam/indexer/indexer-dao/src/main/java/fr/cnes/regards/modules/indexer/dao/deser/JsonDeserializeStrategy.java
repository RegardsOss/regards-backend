package fr.cnes.regards.modules.indexer.dao.deser;

public interface JsonDeserializeStrategy<T> {

    <U extends T> U deserializeJson(String sourceAsString, Class<U> clazz);

}
