package com.mundodetronos2.role;

import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class RoleRegistry {
    private static final Map<String, RoleDefinition> registry = new HashMap<>();

    static {
        register(new RoleDefinition(
                "berserker",
                Component.translatable("gui.mundodetronos2.role_selection.berserker"),
                Component.translatable("gui.mundodetronos2.role_desc.berserker"),
                0x990000 // Rojo oscuro
        ));

        register(new RoleDefinition(
                "warrior",
                Component.translatable("gui.mundodetronos2.role_selection.warrior"),
                Component.translatable("gui.mundodetronos2.role_desc.warrior"),
                0x808080 // Gris/Acero
        ));

        register(new RoleDefinition(
                "mage",
                Component.translatable("gui.mundodetronos2.role_selection.mage"),
                Component.translatable("gui.mundodetronos2.role_desc.mage"),
                0x8A2BE2 // Azul/Violeta
        ));

        register(new RoleDefinition(
                "archer",
                Component.translatable("gui.mundodetronos2.role_selection.archer"),
                Component.translatable("gui.mundodetronos2.role_desc.archer"),
                0x228B22 // Verde
        ));

        register(new RoleDefinition(
                "paladin",
                Component.translatable("gui.mundodetronos2.role_selection.paladin"),
                Component.translatable("gui.mundodetronos2.role_desc.paladin"),
                0xFFD700 // Dorado
        ));

        register(new RoleDefinition(
                "draconico",
                Component.translatable("gui.mundodetronos2.role_selection.draconico"),
                Component.translatable("gui.mundodetronos2.role_desc.draconico"),
                0xD2691E // Rojo oscuro/Ámbar
        ));

        register(new RoleDefinition(
                "clerigo",
                Component.translatable("gui.mundodetronos2.role_selection.clerigo"),
                Component.translatable("gui.mundodetronos2.role_desc.clerigo"),
                0x00FF7F // Verde claro/Blanco
        ));
    }

    public static void register(RoleDefinition role) {
        registry.put(role.getId().toLowerCase(), role);
    }

    public static RoleDefinition getRole(String id) {
        if (id == null) return null;
        // Soporte para IDs antiguos o alternativos
        String clean = id.toLowerCase();
        if (clean.equals("guerrero")) clean = "warrior";
        if (clean.equals("mago")) clean = "mage";
        if (clean.equals("arquero")) clean = "archer";
        return registry.get(clean);
    }

    public static Map<String, RoleDefinition> getRegisteredRoles() {
        return registry;
    }
}
