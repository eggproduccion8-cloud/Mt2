package com.mundodetronos2.throne;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public class FortressWallManager {

    public static Block getWallBlockForLevel(int wallLevel) {
        return switch (wallLevel) {
            case 2 -> Blocks.STONE_BRICKS;
            case 3 -> Blocks.OBSIDIAN;
            default -> Blocks.OAK_PLANKS;
        };
    }

    public static boolean isNaturalOrReplaceable(BlockState state) {
        if (state.isAir() || state.canBeReplaced()) return true;
        Block block = state.getBlock();
        return block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.STONE ||
               block == Blocks.SAND || block == Blocks.GRAVEL || block == Blocks.SNOW ||
               block == Blocks.OAK_LEAVES || block == Blocks.SPRUCE_LEAVES || block == Blocks.BIRCH_LEAVES ||
               block == Blocks.JUNGLE_LEAVES || block == Blocks.ACACIA_LEAVES || block == Blocks.DARK_OAK_LEAVES ||
               block == Blocks.OAK_PLANKS || block == Blocks.STONE_BRICKS || block == Blocks.OBSIDIAN ||
               block == Blocks.WATER || block == Blocks.LAVA;
    }

    public static void buildOrUpdateWall(ServerLevel level, ThroneData throne) {
        if (level == null || throne == null || throne.getPos() == null) return;

        BlockPos center = throne.getPos();
        int cx = center.getX();
        int cz = center.getZ();

        // Remove previous registered wall blocks cleanly
        clearCurrentWall(level, throne);

        Block wallBlock = getWallBlockForLevel(throne.getWallLevel());
        BlockState wallState = wallBlock.defaultBlockState();

        int margin = 71; // 150x150 zone border minus 4 blocks margin
        int minX = cx - margin;
        int maxX = cx + margin;
        int minZ = cz - margin;
        int maxZ = cz + margin;

        List<int[]> newWallBlocks = new ArrayList<>();

        // Generate wall segments along 4 sides with 3-block width depth
        for (int x = minX; x <= maxX; x++) {
            for (int w = -1; x + w >= minX && x + w <= maxX && w <= 1; w++) {
                // North wall (minZ)
                if (!isEntrance(x, cx)) {
                    buildWallColumn(level, x, minZ + w, wallState, newWallBlocks);
                } else {
                    clearEntranceColumn(level, x, minZ + w);
                }
                // South wall (maxZ)
                if (!isEntrance(x, cx)) {
                    buildWallColumn(level, x, maxZ + w, wallState, newWallBlocks);
                } else {
                    clearEntranceColumn(level, x, maxZ + w);
                }
            }
        }

        for (int z = minZ + 1; z < maxZ; z++) {
            for (int w = -1; w <= 1; w++) {
                // West wall (minX)
                if (!isEntrance(z, cz)) {
                    buildWallColumn(level, minX + w, z, wallState, newWallBlocks);
                } else {
                    clearEntranceColumn(level, minX + w, z);
                }
                // East wall (maxX)
                if (!isEntrance(z, cz)) {
                    buildWallColumn(level, maxX + w, z, wallState, newWallBlocks);
                } else {
                    clearEntranceColumn(level, maxX + w, z);
                }
            }
        }

        throne.setWallBlocksRaw(newWallBlocks);
    }

    private static boolean isEntrance(int coord, int centerCoord) {
        // Centered 3-block wide entrance
        return coord >= centerCoord - 1 && coord <= centerCoord + 1;
    }

    private static void buildWallColumn(ServerLevel level, int x, int z, BlockState wallState, List<int[]> wallList) {
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        // Foundation check: Fill gaps/holes under wall down to solid ground (up to 10 blocks deep)
        for (int fillY = groundY - 1; fillY >= Math.max(level.getMinBuildHeight(), groundY - 10); fillY--) {
            BlockPos fillPos = new BlockPos(x, fillY, z);
            BlockState fillState = level.getBlockState(fillPos);
            if (fillState.isAir() || fillState.canBeReplaced() || fillState.is(Blocks.WATER) || fillState.is(Blocks.LAVA)) {
                level.setBlock(fillPos, wallState, 3);
                wallList.add(new int[]{fillPos.getX(), fillPos.getY(), fillPos.getZ()});
            } else {
                break; // Hit firm solid ground
            }
        }

        // Build 8 blocks high above terrain
        for (int dy = 0; dy < 8; dy++) {
            BlockPos pos = new BlockPos(x, groundY + dy, z);
            BlockState cur = level.getBlockState(pos);
            if (isNaturalOrReplaceable(cur)) {
                level.setBlock(pos, wallState, 3);
                wallList.add(new int[]{pos.getX(), pos.getY(), pos.getZ()});
            }
        }
    }

    private static void clearEntranceColumn(ServerLevel level, int x, int z) {
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int dy = 0; dy < 8; dy++) {
            BlockPos pos = new BlockPos(x, groundY + dy, z);
            BlockState cur = level.getBlockState(pos);
            if (isNaturalOrReplaceable(cur) && !cur.isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static void clearCurrentWall(ServerLevel level, ThroneData throne) {
        if (level == null || throne == null) return;
        List<int[]> currentBlocks = throne.getWallBlocksRaw();
        if (currentBlocks != null) {
            for (int[] arr : currentBlocks) {
                if (arr != null && arr.length >= 3) {
                    BlockPos p = new BlockPos(arr[0], arr[1], arr[2]);
                    BlockState cur = level.getBlockState(p);
                    if (cur.is(Blocks.OAK_PLANKS) || cur.is(Blocks.STONE_BRICKS) || cur.is(Blocks.OBSIDIAN)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            throne.clearWallBlocks();
        }
    }
}
