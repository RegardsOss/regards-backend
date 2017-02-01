/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.modules.plugins.domain;

/**
 * Plugin parameter type
 *
 * @author Christophe Mertz
 */
public class PluginParameterType {

    /**
     * The parameter's name
     */
    private String name;

    /**
     * The parameter's type Java
     */
    private String type;

    /**
     * The parameters's type {@link ParamType}.
     */
    private ParamType paramType;

    /**
     * A constructor with the attribute
     * 
     * @param pName
     *            The parameter's name
     * @param pType
     *            The parameter's type Java
     * @param pTypeEnum
     *            The parameter's type (ie PRIMITIVE or PLUGIN)
     * 
     */
    public PluginParameterType(String pName, String pType, ParamType pTypeEnum) {
        super();
        this.name = pName;
        this.type = pType;
        this.paramType = pTypeEnum;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        this.name = pName;
    }

    public String getType() {
        return type;
    }

    public void setType(String pType) {
        this.type = pType;
    }

    public ParamType getParamType() {
        return paramType;
    }

    public void setParamType(ParamType pParamType) {
        this.paramType = pParamType;
    }

    /**
     * An enumeration with PRIMITIVE and PLUGIN value
     * 
     * @author Christophe Mertz
     *
     */
    public enum ParamType {
        /**
         * Parameter type primitif
         */
        PRIMITIVE,
        /**
         * Parameter type plugin
         */
        PLUGIN
    }
}
