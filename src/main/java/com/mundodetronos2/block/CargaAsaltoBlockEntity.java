package com.mundodetronos2.block;

import com.mundodetronos2.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CargaAsaltoBlockEntity extends BlockEntity {

    private int chargeLevel = 1;

    public CargaAsaltoBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.CARGA_ASALTO_BE.get(), pos, state);
    }

    public int getChargeLevel() {
        return chargeLevel;
    }

    public void setChargeLevel(int level) {
        this.chargeLevel = level;
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("ChargeLevel")) {
            this.chargeLevel = tag.getInt("ChargeLevel");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ChargeLevel", this.chargeLevel);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
