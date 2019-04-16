package io.github.teamgalacticraft.galacticraft.blocks.machines.oxygencollector;

import alexiil.mc.lib.attributes.item.impl.SimpleFixedItemInv;
import io.github.cottonmc.energy.api.EnergyAttribute;
import io.github.cottonmc.energy.impl.SimpleEnergyAttribute;
import io.github.prospector.silk.util.ActionType;
import io.github.teamgalacticraft.galacticraft.api.EnergyHolderItem;
import io.github.teamgalacticraft.galacticraft.blocks.machines.MachineBlockEntity;
import io.github.teamgalacticraft.galacticraft.energy.GalacticraftEnergy;
import io.github.teamgalacticraft.galacticraft.entity.GalacticraftBlockEntities;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;

public class OxygenCollectorBlockEntity extends MachineBlockEntity implements Tickable, BlockEntityClientSerializable {
    public CollectorStatus status = CollectorStatus.INACTIVE;
    private SimpleEnergyAttribute oxygen = new SimpleEnergyAttribute(5000, GalacticraftEnergy.GALACTICRAFT_OXYGEN);
    public static int BATTERY_SLOT = 0;
    public int lastCollectAmount = 0;

    public OxygenCollectorBlockEntity() {
        super(GalacticraftBlockEntities.OXYGEN_COLLECTOR_TYPE);
        this.getEnergy().listen(this::markDirty);
    }

    @Override
    protected int getInvSize() {
        return 1;
    }

    private int collectOxygen(BlockPos center) {
        int minX = center.getX() - 5;
        int minY = center.getY() - 5;
        int minZ = center.getZ() - 5;
        int maxX = center.getX() + 5;
        int maxY = center.getY() + 5;
        int maxZ = center.getZ() + 5;

        int leafBlocks = 0;

        for (BlockPos pos : BlockPos.iterateBoxPositions(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isAir()) {
                continue;
            }
            if (blockState.getBlock() instanceof LeavesBlock || blockState.getBlock() instanceof CropBlock) {
                leafBlocks++;
            }
        }

        if (leafBlocks < 2) return 0;
        return leafBlocks;
    }

    @Override
    public void tick() {
        attemptChargeFromStack(getInventory().getInvStack(BATTERY_SLOT));
        lastCollectAmount = collectOxygen(this.pos);
        if (this.getEnergy().getCurrentEnergy() <= 0) {
            this.status = CollectorStatus.INACTIVE;
        }
        if (this.lastCollectAmount <= 0) {
            this.status = CollectorStatus.NOT_ENOUGH_LEAVES;
        } else {
            this.status = CollectorStatus.COLLECTING;
        }

        if (status == CollectorStatus.COLLECTING && this.getOxygen().getMaxEnergy() != this.oxygen.getCurrentEnergy()) {
            this.getEnergy().extractEnergy(GalacticraftEnergy.GALACTICRAFT_JOULES, 1, ActionType.PERFORM);
            this.oxygen.insertEnergy(GalacticraftEnergy.GALACTICRAFT_OXYGEN, collectOxygen(this.pos), ActionType.PERFORM);
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        tag.putInt("Oxygen", oxygen.getCurrentEnergy());

        return tag;
    }
    @Override
    public void fromTag(CompoundTag tag) {
        super.fromTag(tag);

        this.oxygen.setCurrentEnergy(tag.getInt("Oxygen"));
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        this.fromTag(tag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        return this.toTag(tag);
    }

    public EnergyAttribute getOxygen() {
        return this.oxygen;
    }
}