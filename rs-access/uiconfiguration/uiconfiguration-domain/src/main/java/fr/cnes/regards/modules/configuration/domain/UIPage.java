/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * UI Page entity.
 *
 * @author Sébastien Binda
 */
@Embeddable
public class UIPage {

    /**
     * Does the page is the home page of the website ?
     */
    @Column
    private Boolean home = false;

    @Column(length = 128)
    private String iconType;

    @Column(length = 512)
    private String customIconURL;

    @Column(columnDefinition = "text")
    private String title;

    public UIPage() {
        super();
    }

    public UIPage(boolean home, String iconType, String customIconURL, String title) {
        super();
        this.home = home;
        this.iconType = iconType;
        this.customIconURL = customIconURL;
        this.title = title;
    }

    public Boolean isHome() {
        return home;
    }

    public void setHome(Boolean home) {
        this.home = home;
    }

    public String getIconType() {
        return iconType;
    }

    public void setIconType(String iconType) {
        this.iconType = iconType;
    }

    public String getCustomIconURL() {
        return customIconURL;
    }

    public void setCustomIconURL(String customIconURL) {
        this.customIconURL = customIconURL;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
