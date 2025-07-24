/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.model.dto.properties.set;

import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.ObjectProperty;
import fr.cnes.regards.modules.model.dto.properties.StringArrayProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Julien Canches
 */
class PropertySetTest {

    private static final OffsetDateTime DATE = OffsetDateTime.parse("2025-06-25T11:20:34+02:00");

    Set<IProperty<?>> originalSet;

    Set<IProperty<?>> editedSet;

    PropertySet editor;

    List<PropertyChange> changes = new ArrayList<>();

    PropertySet.MergeListener listener = new PropertySet.MergeListener() {

        @Override
        public void propertyAdded(String qualifiedName, Object value) {
            changes.add(new PropertyChange(qualifiedName, null, value));
        }

        @Override
        public void propertyUpdated(String qualifiedName, Object oldValue, Object newValue) {
            changes.add(new PropertyChange(qualifiedName, oldValue, newValue));
        }

        @Override
        public void propertyRemoved(String qualifiedName, Object oldValue) {
            changes.add(new PropertyChange(qualifiedName, oldValue, null));
        }
    };

    private static Set<IProperty<?>> originalSet() {
        return Set.of(IProperty.buildLong("long", 2654L), IProperty.buildString("str", "hello"), objectProperty());
    }

    private static ObjectProperty objectProperty() {
        return IProperty.buildObject("obj",
                                     IProperty.buildDouble("double", Math.PI),
                                     IProperty.buildBoolean("bool", true),
                                     subObjectProperty());
    }

    private static ObjectProperty subObjectProperty() {
        return IProperty.buildObject("subObject",
                                     IProperty.buildStringArray("array", "zero", "one", "two"),
                                     IProperty.buildDate("date", DATE));
    }

    @BeforeEach
    void init() {
        originalSet = originalSet();
        // Below, we call the method originalSet() to make sure we don't reuse the same IProperty instances
        editedSet = new HashSet<>(originalSet());
        editor = new PropertySet(editedSet);
    }

    @Test
    void get() {
        assertThat(editor.get("long")).isEqualTo(IProperty.buildLong("long", 2654L));
        assertThat(editor.get("str")).isEqualTo(IProperty.buildString("str", "hello"));
        assertThat(editor.get("obj.double")).isEqualTo(IProperty.buildDouble("double", Math.PI));
        assertThat(editor.get("obj.bool")).isEqualTo(IProperty.buildBoolean("bool", true));
        assertThat(editor.get("obj.subObject.array")).isEqualTo(IProperty.buildStringArray("array",
                                                                                           "zero",
                                                                                           "one",
                                                                                           "two"));
        assertThat(editor.get("obj.subObject.date")).isEqualTo(IProperty.buildDate("date", DATE));
    }

    @Test
    void getAllUnqualifiedNames() {
        assertThat(editor.getAllQualifiedNames()).containsExactlyInAnyOrder("long",
                                                                            "str",
                                                                            "obj.double",
                                                                            "obj.bool",
                                                                            "obj.subObject.array",
                                                                            "obj.subObject" + ".date");
    }

    @Test
    void mergeNewProperty() {
        Set<IProperty<?>> patch = Set.of(IProperty.buildString("str2", "bye"));
        editor.mergeProperties(patch, listener);
        assertThat(changes).containsExactly(new PropertyChange("str2", null, "bye"));
        assertThat(editedSet).hasSize(originalSet.size() + 1);
        assertThat(editedSet).usingElementComparator(FULL_PROPERTY_COMPARATOR).containsAll(originalSet);
        assertThat(editedSet).usingElementComparator(FULL_PROPERTY_COMPARATOR).containsAll(patch);
        assertThat(editor.get("str2")).isEqualTo(IProperty.buildString("str2", "bye"));
    }

    @Test
    void mergeNewNestedPropertyInNewObject() {
        Set<IProperty<?>> patch = Set.of(IProperty.buildObject("obj2", IProperty.buildString("str2", "bye")));
        editor.mergeProperties(patch, listener);
        assertThat(changes).containsExactly(new PropertyChange("obj2.str2", null, "bye"));
        assertThat(editedSet).hasSize(originalSet.size() + 1);
        assertThat(editedSet).usingElementComparator(FULL_PROPERTY_COMPARATOR).containsAll(originalSet);
        assertThat(editedSet).usingElementComparator(FULL_PROPERTY_COMPARATOR).containsAll(patch);
        assertThat(editor.get("obj2.str2")).isEqualTo(IProperty.buildString("str2", "bye"));
    }

    @Test
    void mergeNewNestedPropertyInExistingObject() {
        Set<IProperty<?>> patch = Set.of(IProperty.buildObject("obj", IProperty.buildString("str2", "bye")));
        editor.mergeProperties(patch, listener);
        assertThat(changes).containsExactly(new PropertyChange("obj.str2", null, "bye"));
        assertThat(editedSet).usingElementComparator(FULL_PROPERTY_COMPARATOR)
                             .containsExactlyInAnyOrder(IProperty.buildLong("long", 2654L),
                                                        IProperty.buildString("str", "hello"),
                                                        IProperty.buildObject("obj",
                                                                              IProperty.buildString("str2", "bye"),
                                                                              IProperty.buildDouble("double", Math.PI),
                                                                              IProperty.buildBoolean("bool", true),
                                                                              subObjectProperty()));
        assertThat(editor.get("obj.str2")).isEqualTo(IProperty.buildString("str2", "bye"));
    }

    @Test
    void mergeRemovedProperty() {
        Set<IProperty<?>> patch = Set.of(IProperty.buildLong("long", null));
        editor.mergeProperties(patch, listener);
        assertThat(changes).containsExactly(new PropertyChange("long", 2654L, null));
        assertThat(editedSet).usingElementComparator(FULL_PROPERTY_COMPARATOR)
                             .containsExactlyInAnyOrder(IProperty.buildLong("long", null),
                                                        IProperty.buildString("str", "hello"),
                                                        objectProperty());
        assertThat(editor.get("long")).isEqualTo(IProperty.buildLong("long", null));
    }

    @Test
    void mergeRemovedPropertyInNestedObject() {
        Set<IProperty<?>> patch = Set.of(IProperty.buildObject("obj", IProperty.buildDouble("double", null)));
        editor.mergeProperties(patch, listener);
        assertThat(changes).containsExactly(new PropertyChange("obj.double", Math.PI, null));
        assertThat(editedSet).usingElementComparator(FULL_PROPERTY_COMPARATOR)
                             .containsExactlyInAnyOrder(IProperty.buildLong("long", 2654L),
                                                        IProperty.buildString("str", "hello"),
                                                        IProperty.buildObject("obj",
                                                                              IProperty.buildString("str", "bye"),
                                                                              IProperty.buildDouble("double", null),
                                                                              IProperty.buildBoolean("bool", true),
                                                                              subObjectProperty()));
        assertThat(editor.get("obj.double")).isEqualTo(IProperty.buildDouble("double", null));
    }

    /**
     * Verifies that the target properties are unchanged when merging a property with a null value into properties that
     * do not already contain this property.
     */
    @Test
    void mergeNullPropertyOnNonExistingProperty() {
        Set<IProperty<?>> patch = Set.of(IProperty.buildLong("i_do_not_exist", null));
        editor.mergeProperties(patch, listener);
        assertThat(changes).isEmpty();
        assertThat(editedSet).usingElementComparator(FULL_PROPERTY_COMPARATOR)
                             .containsExactlyInAnyOrderElementsOf(originalSet);
        assertThat(editor.get("obj.i_do_not_exist")).isNull();
    }

    @Test
    void mergeUpdatedProperty() {
        Set<IProperty<?>> patch = Set.of(IProperty.buildString("str", "hello2"));
        editor.mergeProperties(patch, listener);
        assertThat(changes).containsExactly(new PropertyChange("str", "hello", "hello2"));
        assertThat(editedSet).hasSize(originalSet.size());
        assertThat(editedSet).usingElementComparator(FULL_PROPERTY_COMPARATOR)
                             .containsExactlyInAnyOrder(IProperty.buildLong("long", 2654L),
                                                        IProperty.buildString("str", "hello2"),
                                                        objectProperty());
        assertThat(editor.get("str")).isEqualTo(IProperty.buildString("str", "hello2"));
    }

    @Test
    void mergeUpdatedPropertyInNestedObject() {
        Set<IProperty<?>> patch = Set.of(IProperty.buildObject("obj", IProperty.buildDouble("double", Math.E)));
        editor.mergeProperties(patch, listener);
        assertThat(changes).containsExactly(new PropertyChange("obj.double", Math.PI, Math.E));
        assertThat(editedSet).hasSize(originalSet.size());
        assertThat(editedSet).usingElementComparator(FULL_PROPERTY_COMPARATOR)
                             .containsExactlyInAnyOrder(IProperty.buildLong("long", 2654L),
                                                        IProperty.buildString("str", "hello"),
                                                        IProperty.buildObject("obj",
                                                                              IProperty.buildDouble("double", Math.E),
                                                                              IProperty.buildBoolean("bool", true),
                                                                              subObjectProperty()));
        assertThat(editor.get("obj.double")).isEqualTo(IProperty.buildDouble("double", Math.E));
    }

    private static record PropertyChange(String qualifiedName,
                                         Object oldValue,
                                         Object newValue) {

    }

    /**
     * IProperty.equals() only compares name, not value. For our tests, we want to compare everything.
     * This comparator does not cover all property types, but only those used in the tests. Reuse with care.
     */
    private static final Comparator<IProperty<?>> FULL_PROPERTY_COMPARATOR = new Comparator<>() {

        @Override
        public int compare(IProperty<?> o1, IProperty<?> o2) {
            if (areEqual(o1, o2)) {
                return 0;
            }
            // Try to have a consistent order:
            return Comparator.comparingInt(System::identityHashCode).compare(o1, o2);
        }

        private boolean areEqual(IProperty<?> o1, IProperty<?> o2) {
            if (!o1.equals(o2)) {
                return false;
            }
            if (o1 instanceof StringArrayProperty p1 && o2 instanceof StringArrayProperty p2) {
                return Arrays.equals(p1.getValue(), p2.getValue());
            }
            if (o1 instanceof ObjectProperty oo1 && o2 instanceof ObjectProperty oo2) {
                return areEqualSets(oo1.getValue(), oo2.getValue());
            }
            return Objects.equals(o1.getValue(), o2.getValue());
        }

        private boolean areEqualSets(Set<IProperty<?>> ps1, Set<IProperty<?>> ps2) {
            return ps1.stream().allMatch(p1 -> ps2.stream().anyMatch(p2 -> areEqual(p1, p2)));
        }

    };

}
