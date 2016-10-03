/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;


/**
 * 
 * @author cmertz
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public interface IPair<K, V> {

	/**
	 * 
	 * @return the key
	 */
    K getKey();

	/**
	 * 
	 * @return the value
	 */
    V getValue();

	/**
	 * 
	 * @param pKey set the key
	 * @return the key
	 */
    K setKey(K pKey);

	/**
	 * 
	 * @param pValue set the value
	 * @return the value
	 */
    V setValue(V pValue);

}
