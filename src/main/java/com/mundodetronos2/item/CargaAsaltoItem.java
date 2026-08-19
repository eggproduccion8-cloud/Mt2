package com.mundodetronos2.item;

import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import com.mundodetronos2.throne.ThroneData;
import com.mundodetronos2.throne.ThroneManager;
import com.mundodetronos2.throne.ThroneState;
import com.mundodetronos2.throne.ThroneAttackManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class CargaAsaltoItem extends Item {

    public CargaAsaltoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.FAIL;
        }

        // 1. Verificar si el evento global está activo
        if (!ThroneManager.isGlobalEventActive()) {
            com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ El trono está protegido. No hay ningún evento activo.");
            return InteractionResult.FAIL;
        }

        // Límite de 4 cargas activas
        if (ThroneAttackManager.getActiveAttacksSize() >= 4) {
            com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Se ha alcanzado el límite máximo de 4 cargas activas.");
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockPos placePos = clickedPos.relative(context.getClickedFace());
        BlockState clickedState = level.getBlockState(clickedPos);
        String blockName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(clickedState.getBlock()).toString().toLowerCase();

        // 2. Encontrar el trono más cercano en el radio de 5 bloques
        ThroneData nearestThrone = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (ThroneData throne : ThroneManager.getThronesMap().values()) {
            if (throne.getDimension().equals(level.dimension().location().toString())) {
                BlockPos tPos = throne.getPos();
                if (tPos != null) {
                    double distSq = sp.distanceToSqr(tPos.getX() + 0.5D, tPos.getY() + 0.5D, tPos.getZ() + 0.5D);
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearestThrone = throne;
                    }
                }
            }
        }

        boolean isThroneAttack = nearestThrone != null && nearestDistSq <= 25.0D;

        boolean isWallStructure = isVerticalWallStructure(level, clickedPos, context.getClickedFace(), clickedState, blockName);

        if (!isThroneAttack && !isWallStructure) {
            com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Esta Carga de Asalto no puede destruir ese bloque.");
            return InteractionResult.FAIL;
        }

        UUID playerId = sp.getUUID();
        RealmData playerRealm = RealmManager.getPlayerRealm(playerId);

        // 3. Validaciones de equipo / terreno propio
        if (isThroneAttack) {
            if (nearestThrone.getState() == ThroneState.REPAIRING) {
                com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ El trono se está reparando actualmente.");
                return InteractionResult.FAIL;
            }

            if (playerRealm != null && playerRealm.getId().equals(nearestThrone.getRealmId())) {
                com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ No puedes atacar tu propio Trono.");
                return InteractionResult.FAIL;
            }

            if (ThroneAttackManager.isUnderAttack(nearestThrone.getId())) {
                com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Este trono ya está bajo ataque con una carga activa.");
                return InteractionResult.FAIL;
            }
        }

        // 4. Colocar la carga
        BlockState targetState = level.getBlockState(placePos);
        if (!targetState.isAir() && !targetState.canBeReplaced()) {
            placePos = clickedPos;
        }

        level.setBlockAndUpdate(placePos, com.mundodetronos2.init.BlockInit.CARGA_ASALTO_BLOCK.get().defaultBlockState());

        // Registrar el ataque en el manager
        String attackerTeamName = "Sin Equipo";
        if (playerRealm != null) {
            attackerTeamName = playerRealm.getName();
        }

        if (isThroneAttack) {
            ThroneAttackManager.startAttack(nearestThrone.getId(), placePos, level.dimension().location().toString(), playerId, attackerTeamName);
            com.mundodetronos2.network.MessageManager.actionBar(sp, "§a✔ Carga de asalto colocada en el Trono. Detonando en 30 segundos.");
        } else {
            // Muro / estructura vertical: asociar la carga directamente al clickedPos (bloque objetivo real)
            ThroneAttackManager.ChargeGroup group = ThroneAttackManager.registerBlockCharge(level.dimension().location().toString(), clickedPos, placePos, clickedState, playerId, attackerTeamName);
            com.mundodetronos2.network.MessageManager.actionBar(sp, "§a✔ Carga de asalto colocada en muro (" + group.currentCharges + " / " + group.requiredCharges + " cargas). Detonando en 10s.");
        }

        // Sonido de mecha de TNT
        level.playSound(null, placePos, net.minecraft.sounds.SoundEvents.TNT_PRIMED, net.minecraft.sounds.SoundSource.BLOCKS, 1.2F, 1.0F);

        // Consumir item
        context.getItemInHand().shrink(1);

        return InteractionResult.SUCCESS;
    }

    private boolean isVerticalWallStructure(Level level, BlockPos pos, net.minecraft.core.Direction clickedFace, BlockState state, String blockName) {
        // Excluir terreno natural del suelo y decoraciones horizontales
        if (blockName.contains("dirt") || blockName.contains("grass") || blockName.contains("sand") ||
            blockName.contains("gravel") || blockName.contains("mud") || blockName.contains("clay") ||
            blockName.contains("farmland") || blockName.contains("path") || blockName.contains("podzol") ||
            blockName.contains("mycelium") || blockName.contains("terracotta") || blockName.contains("concrete") ||
            blockName.contains("carpet") || blockName.contains("flower") || blockName.contains("foliage") ||
            blockName.contains("snow") || blockName.contains("ice")) {
            return false;
        }

        // Materiales estructurales permitidos (Madera, Piedra, Obsidiana)
        boolean isWood = blockName.contains("wood") || blockName.contains("log") || blockName.contains("planks") || blockName.contains("fence") || blockName.contains("slab") || blockName.contains("stairs") || state.is(net.minecraft.tags.BlockTags.PLANKS);
        boolean isStone = blockName.contains("stone") || blockName.contains("cobble") || blockName.contains("andesite") || blockName.contains("granite") || blockName.contains("diorite") || blockName.contains("deepslate") || blockName.contains("brick") || blockName.contains("basalt") || blockName.contains("obsidian");

        if (!isWood && !isStone) {
            return false;
        }

        // Comprobar que forma parte de una estructura vertical/pared
        boolean sideClick = clickedFace.getAxis().isHorizontal();
        BlockState aboveState = level.getBlockState(pos.above());
        BlockState belowState = level.getBlockState(pos.below());
        String aboveName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(aboveState.getBlock()).toString().toLowerCase();
        String belowName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(belowState.getBlock()).toString().toLowerCase();

        boolean hasVerticalNeighbor = (aboveState.isSolid() && !aboveName.contains("dirt") && !aboveName.contains("grass")) ||
                                      (belowState.isSolid() && !belowName.contains("dirt") && !belowName.contains("grass"));

        return sideClick || hasVerticalNeighbor;
    }
}
