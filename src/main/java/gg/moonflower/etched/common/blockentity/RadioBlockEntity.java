package gg.moonflower.etched.common.blockentity;

import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.radio.RadioClientBridge;
import gg.moonflower.etched.common.radio.RadioConfiguration;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Ocelot
 */
public class RadioBlockEntity extends BlockEntity implements Clearable {

    private String url;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(EtchedBlocks.RADIO_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioBlockEntity blockEntity) {
        RadioClientBridge.tick(level, pos, blockEntity.getConfiguration(state));
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.url = nbt.contains("Url", Tag.TAG_STRING) ? nbt.getString("Url") : null;
        this.publishUpdate();
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        if (this.url != null) {
            nbt.putString("Url", this.url);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void clearContent() {
        this.url = null;
        this.publishUpdate();
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        if (!Objects.equals(this.url, url)) {
            this.url = url;
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
            this.publishUpdate();
        }
    }

    public boolean isConfiguredAndEnabled() {
        return this.getConfiguration(this.getBlockState()).isEnabled();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.publishUpdate();
    }

    @Override
    public void onChunkUnloaded() {
        this.publishRemove();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        this.publishRemove();
        super.setRemoved();
    }

    @Override
    public void setBlockState(BlockState state) {
        boolean powered = this.getConfiguration(this.getBlockState()).powered();
        super.setBlockState(state);
        if (powered != this.getConfiguration(state).powered()) {
            this.publishUpdate();
        }
    }

    private RadioConfiguration getConfiguration(BlockState state) {
        boolean powered = state.hasProperty(RadioBlock.POWERED) && state.getValue(RadioBlock.POWERED);
        return new RadioConfiguration(this.url, powered);
    }

    private void publishUpdate() {
        if (this.level != null) {
            RadioClientBridge.update(this.level, this.worldPosition, this.getConfiguration(this.getBlockState()));
        }
    }

    private void publishRemove() {
        if (this.level != null) {
            RadioClientBridge.remove(this.level, this.worldPosition);
        }
    }
}
