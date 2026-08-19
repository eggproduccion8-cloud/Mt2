package com.mundodetronos2.item;

import com.mundodetronos2.network.MessageManager;
import com.mundodetronos2.progression.PlayerProgressData;
import com.mundodetronos2.progression.ProgressionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MejoraMochilaItem extends Item {

    public MejoraMochilaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PlayerProgressData pData = ProgressionManager.getProgressData(serverPlayer.getUUID());
            int currentTier = pData.getBackpackTier();

            if (currentTier >= 3) {
                MessageManager.actionBar(serverPlayer, "§c⚠ Tu mochila ya está al máximo.");
                return InteractionResultHolder.fail(stack);
            }

            int nextTier = currentTier + 1;
            pData.setBackpackTier(nextTier);
            ProgressionManager.save(true);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (nextTier == 2) {
                MessageManager.actionBar(serverPlayer, "§a✔ ¡Mochila mejorada! Ahora tienes 30 espacios.");
            } else if (nextTier == 3) {
                MessageManager.actionBar(serverPlayer, "§a✔ ¡Mochila mejorada al máximo! Ahora tienes 45 espacios.");
            }
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
