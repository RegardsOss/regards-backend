/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import java.io.Serializable;
import java.util.List;

public interface GenericDao<K extends Serializable, T> {

    public T find(K id);

    public List<T> find();

    public K save(T value);

    public void update(T value);

    public void delete(T value);
}