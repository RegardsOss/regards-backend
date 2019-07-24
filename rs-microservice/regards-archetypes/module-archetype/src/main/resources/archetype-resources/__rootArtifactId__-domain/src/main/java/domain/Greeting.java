#set($symbol_pound='#')
        #set($symbol_dollar='$')
        #set($symbol_escape='\' )
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
        package ${package}.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
/**
 * TODO Description
 * @author TODO
 */
@Entity
@Table(name = "t_greeting")
public class Greeting {

    /**
     * Id is datasource id (see table pluginConf)
     */
    @Id
    @Column(name = "ds_id")
    private Long id;

    @Column(name = "t_content")
    private String content_;

    public Greeting() {
        super();
    }

    public Greeting(String pName) {
        this.content_ = "Hello " + pName;
    }

    public String getContent() {
        return content_;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the content_
     */
    public String getContent_() {
        return content_;
    }

    /**
     * @param content_ the content_ to set
     */
    public void setContent_(String content_) {
        this.content_ = content_;
    }

}
