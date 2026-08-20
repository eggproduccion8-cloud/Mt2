package com.mundodetronos2.item;

import com.mundodetronos2.block.CrateBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CrateBlockItem extends BlockItem {
    private final int crateLevel;

    public CrateBlockItem(Block block, int crateLevel, Properties properties) {
        super(block, properties);
        this.crateLevel = crateLevel;
    }

    public int getCrateLevel() {
        return crateLevel;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        boolean placed = super.placeBlock(context, state.setValue(CrateBlock.LEVEL, this.crateLevel));
        if (placed) {
            ItemStack stack = context.getItemInHand();
            stack.getOrCreateTag().putInt("mundodetronos2:crate_level", this.crateLevel);
        }
        return placed;
    }
}
