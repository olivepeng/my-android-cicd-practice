package com.digifly.uart.type;


/**
 * BAT_mode(Status) | Power Key Press
 * Power On | BMODE_IDLE = 0xF0
 * Power On | BMODE_CHARGING = 0xF1
 * Power On | BMODE_FULL = 0xF2
 * Power On | BMODE_DISCHARGING = 0xF3
 * --------------------------------------------------
 * BAT_mode(Status) | Power key release
 * Power On | BMODE_IDLE = 0xE0
 * Power On | BMODE_CHARGING = 0xE1
 * Power On | BMODE_FULL = 0xE2
 * Power On | BMODE_DISCHARGING = 0xE3
 */
public enum UartBatModeType {

    // Power Key Press
    BMODE_IDLE_POWER_KEY_PRESS((byte) 0xf0),
    BMODE_CHARGING_POWER_KEY_PRESS((byte) 0xf1),
    BMODE_FULL_POWER_KEY_PRESS((byte) 0xf2),
    BMODE_DISCHARGING_POWER_KEY_PRESS((byte) 0xf3),

    // Power key release
    BMODE_IDLE_POWER_KEY_RELEASE((byte) 0xe0),
    BMODE_CHARGING_POWER_KEY_RELEASE((byte) 0xe1),
    BMODE_FULL_POWER_KEY_RELEASE((byte) 0xe2),
    BMODE_DISCHARGING_POWER_KEY_RELEASE((byte) 0xe3);

    private final byte value;

    UartBatModeType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
