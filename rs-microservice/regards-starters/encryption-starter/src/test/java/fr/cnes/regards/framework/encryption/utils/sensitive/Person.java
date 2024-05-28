/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.encryption.utils.sensitive;

import fr.cnes.regards.framework.encryption.sensitive.StringSensitive;

import java.util.Objects;

/**
 * @author Iliana Ghazali
 **/
public final class Person {

    private final String name;

    @StringSensitive
    private final String password;

    private final Coordinates coordinates;

    public Person(String name, String password, Coordinates coordinates) {
        this.name = name;
        this.password = password;
        this.coordinates = coordinates;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (Person) obj;
        return Objects.equals(this.name, that.name) && Objects.equals(this.password, that.password) && Objects.equals(
            this.coordinates,
            that.coordinates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, password, coordinates);
    }

    @Override
    public String toString() {
        return "Person[" + "name=" + name + ", " + "password=" + password + ", " + "coordinates=" + coordinates + ']';
    }

}

final class Coordinates {

    @StringSensitive
    private final String secretEmailAddress;

    private final SecretLocation secretLocation;

    Coordinates(String secretEmailAddress, SecretLocation secretLocation) {
        this.secretEmailAddress = secretEmailAddress;
        this.secretLocation = secretLocation;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (Coordinates) obj;
        return Objects.equals(this.secretEmailAddress, that.secretEmailAddress) && Objects.equals(this.secretLocation,
                                                                                                  that.secretLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretEmailAddress, secretLocation);
    }

    @Override
    public String toString() {
        return "Coordinates{"
               + "secretEmailAddress='"
               + secretEmailAddress
               + '\''
               + ", location="
               + secretLocation
               + '}';
    }
}

final class SecretLocation {

    private final String city;

    @StringSensitive
    private final String secretAddress;

    SecretLocation(String city, String secretAddress) {
        this.city = city;
        this.secretAddress = secretAddress;
    }

    public String getCity() {
        return city;
    }

    public String getSecretAddress() {
        return secretAddress;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SecretLocation) obj;
        return Objects.equals(this.city, that.city) && Objects.equals(this.secretAddress, that.secretAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, secretAddress);
    }

    @Override
    public String toString() {
        return "Location[" + "city=" + city + ", " + "secretAddress=" + secretAddress + ']';
    }

}