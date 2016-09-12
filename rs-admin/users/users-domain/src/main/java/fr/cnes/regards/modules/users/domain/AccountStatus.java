package fr.cnes.regards.modules.users.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountStatus {
    INACTIVE, ACCEPTED, ACTIVE, LOCKED, PENDING;

    private static Map<String, AccountStatus> namesMap = new HashMap<>(5);

    static {
        namesMap.put("inactive", AccountStatus.INACTIVE);
        namesMap.put("accepted", AccountStatus.ACCEPTED);
        namesMap.put("active", AccountStatus.ACTIVE);
        namesMap.put("locked", AccountStatus.LOCKED);
        namesMap.put("pending", AccountStatus.PENDING);
    }

    @JsonCreator
    public static AccountStatus forValue(String value) {
        return namesMap.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
        for (Entry<String, AccountStatus> entry : namesMap.entrySet()) {
            if (entry.getValue() == this) {
                return entry.getKey();
            }
        }

        return null; // or fail
    }
}
