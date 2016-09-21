/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

/**
 * @author cmertz
 *
 */
public interface IPair<K, V> {

	public K getKey();

	public V getValue();

	public K setKey(K pKey);

	public V setValue(V pValue);

}
