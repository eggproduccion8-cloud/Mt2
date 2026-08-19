package com.mundodetronos2.block;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RoleAnvilBlock extends Block {
    public RoleAnvilBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (player instanceof ServerPlayer sp) {
                // Enviar paquete S2C para abrir la interfaz del Yunque de Roles
                NetworkManager.sendToPlayer(new NetworkManager.S2COpenRoleAnvilPacket(), sp);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
