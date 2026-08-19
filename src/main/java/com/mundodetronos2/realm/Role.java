package com.mundodetronos2.realm;

import java.util.UUID;

public enum Role {
    OWNER,
    MEMBER;

    public static Role fromString(String str) {
        try {
            return Role.valueOf(str.toUpperCase());
        } catch (Exception e) {
            return MEMBER;
        }
    }
}
