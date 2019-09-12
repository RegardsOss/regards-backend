/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.modules.plugins.rest;

import java.util.List;
import java.util.Map;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * Fake SIP validation for testing purpose. Do not use in production!
 * @author Marc Sordi
 */
@Plugin(author = "REGARDS Team", description = "Plugin for plugin parameter type testing", id = "ParamTestPlugin",
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class ParamTestPlugin implements IParamTestPlugin {

    @PluginParameter(label = "Simple string", description = "Simple string description")
    private String pString;

    @PluginParameter(label = "Simple byte", description = "Simple byte description")
    private Byte pByte;

    @PluginParameter(label = "Simple short", description = "Simple short description")
    private Short pShort;

    @PluginParameter(label = "Simple integer", description = "Simple integer description")
    private Integer pInteger;

    @PluginParameter(label = "Simple long", description = "Simple long description")
    private Long pLong;

    @PluginParameter(label = "Simple float", description = "Simple float description")
    private Float pFloat;

    @PluginParameter(label = "Simple double", description = "Simple double description")
    private Double pDouble;

    @PluginParameter(label = "Simple boolean", description = "Simple boolean description")
    private Boolean pBoolean;

    @PluginParameter(label = "List of string", description = "List of string description")
    private List<String> sList;

    @PluginParameter(keylabel = "ssMapKey", label = "Map string to string",
            description = "Map string to string description")
    private Map<String, String> ssMap;

    @PluginParameter(label = "Pojo containing string")
    private Pojo pojo;

    @PluginParameter(label = "Constraint pojo wrapper")
    private Constraints constraints;

    @PluginParameter(keylabel = "scMapKey", label = "Constraint map")
    private Map<String, Constraint> scMap;

    @PluginParameter(keylabel = "innerPlugin", label = "innerPlugin")
    private IParamTestPlugin innerPlugin;

    @Override
    public void doIt() {
        // Nothing to do
    }

    public class Pojo {

        String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public class Constraints {

        @PluginParameter(label = "List of constraints")
        private List<Constraint> constraints;

        public List<Constraint> getConstraints() {
            return constraints;
        }

        public void setConstraints(List<Constraint> constraints) {
            this.constraints = constraints;
        }
    }

    public class Constraint {

        @PluginParameter(label = "Pattern", description = "JAVA regular expression")
        private String pattern;

        @PluginParameter(label = "Enabled", description = "Contraint may be enabled/disabled", optional = true,
                defaultValue = "true")
        private boolean enabled;

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public String getpString() {
        return pString;
    }

    public void setpString(String pString) {
        this.pString = pString;
    }

    public Byte getpByte() {
        return pByte;
    }

    public void setpByte(Byte pByte) {
        this.pByte = pByte;
    }

    public Short getpShort() {
        return pShort;
    }

    public void setpShort(Short pShort) {
        this.pShort = pShort;
    }

    public Integer getpInteger() {
        return pInteger;
    }

    public void setpInteger(Integer pInteger) {
        this.pInteger = pInteger;
    }

    public Long getpLong() {
        return pLong;
    }

    public void setpLong(Long pLong) {
        this.pLong = pLong;
    }

    public Float getpFloat() {
        return pFloat;
    }

    public void setpFloat(Float pFloat) {
        this.pFloat = pFloat;
    }

    public Double getpDouble() {
        return pDouble;
    }

    public void setpDouble(Double pDouble) {
        this.pDouble = pDouble;
    }

    public Boolean getpBoolean() {
        return pBoolean;
    }

    public void setpBoolean(Boolean pBoolean) {
        this.pBoolean = pBoolean;
    }

    //    public List<String> getsList() {
    //        return sList;
    //    }
    //
    //    public void setsList(List<String> sList) {
    //        this.sList = sList;
    //    }
    //
    //    public Map<String, String> getSsMap() {
    //        return ssMap;
    //    }
    //
    //    public void setSsMap(Map<String, String> ssMap) {
    //        this.ssMap = ssMap;
    //    }
    //
    //    public Pojo getPojo() {
    //        return pojo;
    //    }
    //
    //    public void setPojo(Pojo pojo) {
    //        this.pojo = pojo;
    //    }
    //
    //    public Constraints getConstraints() {
    //        return constraints;
    //    }
    //
    //    public void setConstraints(Constraints constraints) {
    //        this.constraints = constraints;
    //    }
    //
    //    public Map<String, Constraint> getScMap() {
    //        return scMap;
    //    }
    //
    //    public void setScMap(Map<String, Constraint> scMap) {
    //        this.scMap = scMap;
    //    }

    public IParamTestPlugin getInnerPlugin() {
        return innerPlugin;
    }

    public void setInnerPlugin(IParamTestPlugin innerPlugin) {
        this.innerPlugin = innerPlugin;
    }
}
