package com.mundodetronos2.item;

import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import com.mundodetronos2.role.PlayerRoleData;
import com.mundodetronos2.role.RoleManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class RoleCardItem extends Item {

    public RoleCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player instanceof ServerPlayer sp) {
                CompoundTag tag = stack.getOrCreateTag();
                if (!tag.contains("OwnerUUID")) {
                    // Si no tiene dueño, vincularlo al jugador actual
                    tag.putString("OwnerUUID", sp.getUUID().toString());
                    tag.putString("OwnerName", sp.getGameProfile().getName());

                    PlayerRoleData data = RoleManager.getPlayerRoleData(sp.getUUID());
                    tag.putString("RoleID", data.getRole().name().toLowerCase());
                    tag.putInt("Level", data.getLevel());
                }

                String ownerUuidStr = tag.getString("OwnerUUID");
                String ownerName = tag.getString("OwnerName");

                // Seguridad: El carnet está vinculado al UUID del jugador.
                // Si otro jugador lo usa, rechazar y mandar mensaje.
                if (!ownerUuidStr.equals(sp.getUUID().toString())) {
                    com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Este carnet pertenece a " + ownerName + ".");
                    return InteractionResultHolder.fail(stack);
                }

                // El jugador que hace clic es el dueño. Consultar el servidor (verdad de los datos)
                PlayerRoleData roleData = RoleManager.getPlayerRoleData(sp.getUUID());
                if (!roleData.isHasRole()) {
                    com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ No tienes ningún rol asignado. Visita el altar.");
                    return InteractionResultHolder.fail(stack);
                }

                // Sincronizar y abrir carnet
                RealmData realm = RealmManager.getPlayerRealm(sp.getUUID());
                String rName = realm != null ? realm.getName() : "Ninguno";
                String rRole = realm != null ? (realm.getOwnerId().equals(sp.getUUID()) ? "Líder" : "Miembro") : "N/A";

                com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(sp.getUUID());
                int currentXp = pData.getXp();
                int neededXp = com.mundodetronos2.progression.LevelSystem.getXpNeeded(pData.getLevel());

                NetworkManager.S2CRoleCardDataPacket pkt = new NetworkManager.S2CRoleCardDataPacket(
                        sp.getUUID(),
                        sp.getGameProfile().getName(),
                        roleData.getRole().name(),
                        roleData.getLevel(),
                        rName,
                        rRole,
                        currentXp,
                        neededXp
                );
                NetworkManager.sendToPlayer(pkt, sp);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("RoleID")) {
            String roleId = tag.getString("RoleID").toLowerCase();
            // Mapear IDs de roles en inglés para compatibilidad y localización
            if (roleId.equals("warrior")) roleId = "guerrero";
            if (roleId.equals("mage")) roleId = "mago";
            if (roleId.equals("archer")) roleId = "arquero";
            return Component.translatable("item.mundodetronos2.role_card." + roleId);
        }
        return Component.translatable("item.mundodetronos2.role_card.unassigned");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            String ownerName = tag.getString("OwnerName");
            String roleId = tag.getString("RoleID");
            int lvl = tag.getInt("Level");

            tooltip.add(Component.literal("§7Propietario: §e" + ownerName));
            tooltip.add(Component.literal("§7Rol: §b" + roleId.toUpperCase()));
            tooltip.add(Component.literal("§7Nivel: §a" + lvl));
        } else {
            tooltip.add(Component.translatable("item.mundodetronos2.role_card.tooltip_unassigned"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
