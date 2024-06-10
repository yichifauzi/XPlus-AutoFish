package com.wudji.xplusautofish.mointor;

import com.wudji.xplusautofish.XPlusAutofish;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.predicate.BlockMaterialPredicate;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;

public class FishMonitorMPMotion implements FishMonitorMP{
    // The threshold of detecting a bobber moving downwards, to detect as a fish.
    public static final int PACKET_MOTION_Y_THRESHOLD = -350;

    // Start catching fish after a 1 second threshold of hitting water.
    public static final int START_CATCHING_AFTER_THRESHOLD = 1000;

    // True if the bobber is in the water.
    private boolean hasHitWater = false;

    // Time at which bobber begins to rise in the water.
    // 0 if the bobber has not rose in the water yet.
    private long bobberRiseTimestamp = 0;


    @Override
    public void hookTick(XPlusAutofish autofish, Minecraft minecraft, FishingHook hook) {
        if (worldContainsBlockWithMaterial(hook.level, hook.getBoundingBox(), Material.WATER)) {
            hasHitWater = true;
        }
    }

    @Override
    public void handleHookRemoved() {
        hasHitWater = false;
        bobberRiseTimestamp = 0;
    }

    @Override
    public void handlePacket(XPlusAutofish autofish, Packet<?> packet, Minecraft minecraft) {
        if (packet instanceof ClientboundSetEntityMotionPacket) {
            ClientboundSetEntityMotionPacket velocityPacket = (ClientboundSetEntityMotionPacket) packet;
            if (minecraft.player != null && minecraft.player.fishing != null && minecraft.player.fishing.getId() == velocityPacket.getId()) {

                // Wait until the bobber has rose in the water.
                // Prevent remarking the bobber rise timestamp until it is reset by catching.
                if (hasHitWater && bobberRiseTimestamp == 0 && velocityPacket.getYa() > 0) {
                    // Mark the time in which the bobber began to rise.
                    bobberRiseTimestamp = autofish.timeMillis;
                }

                // Calculate the time in which the bobber has been in the water
                long timeInWater = autofish.timeMillis - bobberRiseTimestamp;

                // If the bobber has been in the water long enough, start detecting the bobber movement.
                if (hasHitWater && bobberRiseTimestamp != 0 && timeInWater > START_CATCHING_AFTER_THRESHOLD) {
                    if (velocityPacket.getXa() == 0 && velocityPacket.getZa() == 0 && velocityPacket.getYa() < PACKET_MOTION_Y_THRESHOLD) {
                        // Catch the fish
                        autofish.catchFish();

                        // Reset the class attributes to default.
                        this.handleHookRemoved();
                    }
                }
            }
        }
    }

    public static boolean worldContainsBlockWithMaterial(Level level, AABB aabb, Material material) {
        int i = Mth.floor(aabb.minX);
        int j = Mth.ceil(aabb.maxX);
        int k = Mth.floor(aabb.minY);
        int l = Mth.ceil(aabb.maxY);
        int m = Mth.floor(aabb.minZ);
        int n = Mth.ceil(aabb.maxZ);
        BlockMaterialPredicate materialPredicate = BlockMaterialPredicate.forMaterial(material);
        return BlockPos.betweenClosedStream(i, k, m, j - 1, l - 1, n - 1).anyMatch((blockPos) -> {
            return materialPredicate.test(level.getBlockState(blockPos));
        });
    }
}
