package fr.cnes.regards.modules.users.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CodeType {
    RESET, UNLOCK;

    private static Map<String, CodeType> namesMap = new HashMap<>(2);

    static {
        namesMap.put("reset", CodeType.RESET);
        namesMap.put("unlock", CodeType.UNLOCK);
    }

    @JsonCreator
    public static CodeType forValue(String value) {
        return namesMap.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
        for (Entry<String, CodeType> entry : namesMap.entrySet()) {
            if (entry.getValue() == this) {
                return entry.getKey();
            }
        }

        return null; // or fail
    }
}
