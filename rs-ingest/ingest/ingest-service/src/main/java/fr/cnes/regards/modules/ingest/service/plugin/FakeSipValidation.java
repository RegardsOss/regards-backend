/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.plugin;

import java.util.List;
import java.util.Map;

import org.springframework.validation.Errors;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipValidation;

/**
 * Fake SIP validation for testing purpose. Do not use in production!
 * @author Marc Sordi
 *
 */
@Plugin(author = "REGARDS Team", description = "Fake no effect SIP validation plugin (do not use in production)",
        id = "FakeSipValidation", version = "1.0.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class FakeSipValidation implements ISipValidation {

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

    @PluginParameter(keylabel = "Map string to string key", label = "Map string to string value",
            description = "Map string to string description")
    private Map<String, String> ssMap;

    @PluginParameter(label = "Pojo containing string")
    private Pojo pojo;

    @PluginParameter(label = "Constraint pojo wrapper")
    private Constraints constraints;

    @PluginParameter(keylabel = "scMap key", label = "scMap value")
    private Map<String, Constraint> scMap;

    @PluginParameter(label = "Embedded plugin")
    private ISipValidation embedded;

    @Override
    public void validate(SIP sip, Errors errors) {
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

}
