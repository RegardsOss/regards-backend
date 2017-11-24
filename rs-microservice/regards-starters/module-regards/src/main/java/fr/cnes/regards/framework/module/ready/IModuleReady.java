package fr.cnes.regards.framework.module.ready;

/**
 * Interface to be implemented when module requires some configuration before actions can be realized.<br/>
 * For exemple:
 * <ul>
 *     <li>storage: an allocation strategy and at least one Data Storage have to be configured</li>
 * </ul>
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IModuleReady {

    ModuleReadiness isReady();

}
