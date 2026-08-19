package com.mundodetronos2.throne;

public enum ThroneState {
    PROTECTED, // El trono está protegido (evento inactivo)
    ACTIVE,    // El trono es atacable (evento activo)
    REPAIRING, // El trono está en reparación / cooldown
    DISABLED;  // Deshabilitado administrativamente

    public static ThroneState fromString(String str) {
        try {
            return ThroneState.valueOf(str.toUpperCase());
        } catch (Exception e) {
            return PROTECTED;
        }
    }
}
