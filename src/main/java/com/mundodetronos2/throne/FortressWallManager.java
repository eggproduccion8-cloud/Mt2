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

        // Generate wall segments along 4 sides
        for (int x = minX; x <= maxX; x++) {
            // North wall (minZ)
            if (!isEntrance(x, cx)) {
                buildWallColumn(level, x, minZ, wallState, newWallBlocks);
            }
            // South wall (maxZ)
            if (!isEntrance(x, cx)) {
                buildWallColumn(level, x, maxZ, wallState, newWallBlocks);
            }
        }

        for (int z = minZ + 1; z < maxZ; z++) {
            // West wall (minX)
            if (!isEntrance(z, cz)) {
                buildWallColumn(level, minX, z, wallState, newWallBlocks);
            }
            // East wall (maxX)
            if (!isEntrance(z, cz)) {
                buildWallColumn(level, maxX, z, wallState, newWallBlocks);
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
        // Build 4 blocks high above terrain
        for (int dy = 0; dy < 4; dy++) {
            BlockPos pos = new BlockPos(x, groundY + dy, z);
            // Don't overwrite existing player blocks or throne
            BlockState cur = level.getBlockState(pos);
            if (cur.isAir() || cur.canBeReplaced() || cur.is(Blocks.OAK_PLANKS) || cur.is(Blocks.STONE_BRICKS) || cur.is(Blocks.OBSIDIAN)) {
                level.setBlock(pos, wallState, 3);
                wallList.add(new int[]{pos.getX(), pos.getY(), pos.getZ()});
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
