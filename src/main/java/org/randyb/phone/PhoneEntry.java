package org.randyb.phone;

import java.util.Map;

public class PhoneEntry {
    public final boolean enabled;
    public final String name;
    public final long time;
    public final String number;
    public final Map<String, Boolean> days;

    public PhoneEntry(boolean enabled,
                      String name,
                      long time,
                      String number,
                      Map<String, Boolean> days) {
        this.enabled = enabled;
        this.name = name;
        this.time = time;
        this.number = number;
        this.days = days;
    }
}
