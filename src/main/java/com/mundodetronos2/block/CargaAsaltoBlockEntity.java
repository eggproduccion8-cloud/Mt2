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
    private int chargeCount = 1;

    public CargaAsaltoBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.CARGA_ASALTO_BE.get(), pos, state);
    }

    public int getChargeLevel() {
        return chargeLevel;
    }

    public void setChargeLevel(int lvl) {
        this.chargeLevel = lvl;
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getChargeCount() {
        return chargeCount;
    }

    public void setChargeCount(int count) {
        this.chargeCount = Math.min(3, Math.max(1, count));
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean addCharge() {
        if (chargeCount < 3) {
            chargeCount++;
            setChanged();
            if (this.level != null && !this.level.isClientSide) {
                this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("ChargeLevel")) {
            this.chargeLevel = tag.getInt("ChargeLevel");
        }
        if (tag.contains("ChargeCount")) {
            this.chargeCount = tag.getInt("ChargeCount");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ChargeLevel", this.chargeLevel);
        tag.putInt("ChargeCount", this.chargeCount);
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
