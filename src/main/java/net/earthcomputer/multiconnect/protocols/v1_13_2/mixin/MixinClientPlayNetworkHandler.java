package net.earthcomputer.multiconnect.protocols.v1_13_2.mixin;

import net.earthcomputer.multiconnect.api.Protocols;
import net.earthcomputer.multiconnect.impl.ConnectionInfo;
import net.earthcomputer.multiconnect.protocols.v1_13_2.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TraderOfferList;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.ArrayList;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {

    @Shadow @Final private static Logger LOGGER;
    @Shadow private ClientWorld world;

    @Shadow public abstract void onLightUpdate(LightUpdateS2CPacket packet);

    @Shadow public abstract void onOpenWrittenBook(OpenWrittenBookS2CPacket packet);

    @Shadow public abstract void onDifficulty(DifficultyS2CPacket packet);

    @Shadow public abstract void onSetTradeOffers(SetTradeOffersS2CPacket packet);

    @Shadow public abstract void onVelocityUpdate(EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket_1);

    @Unique private int lastCenterX;
    @Unique private int lastCenterZ;

    @Inject(method = "onChunkData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;getReadBuffer()Lnet/minecraft/network/PacketByteBuf;", shift = At.Shift.AFTER))
    private void onChunkDataPost(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_13_2) {
            if (!PendingChunkDataPackets.isProcessingQueuedPackets()) {
                LightUpdateS2CPacket lightPacket = new LightUpdateS2CPacket();
                //noinspection ConstantConditions
                LightUpdatePacketAccessor lightPacketAccessor = (LightUpdatePacketAccessor) lightPacket;

                PendingLightData lightData = PendingLightData.getInstance(packet.getX(), packet.getZ());

                lightPacketAccessor.setChunkX(packet.getX());
                lightPacketAccessor.setChunkZ(packet.getZ());

                int blockLightMask = packet.getVerticalStripBitmask() << 1;
                int skyLightMask = world.getDimension().hasSkyLight() ? blockLightMask : 0;
                lightPacketAccessor.setBlockLightMask(blockLightMask);
                lightPacketAccessor.setSkyLightMask(skyLightMask);
                lightPacketAccessor.setBlockLightUpdates(new ArrayList<>());
                lightPacketAccessor.setSkyLightUpdates(new ArrayList<>());

                for (int i = 0; i < 16; i++) {
                    byte[] blockData = lightData.getBlockLight(i);
                    if (blockData != null)
                        lightPacket.getBlockLightUpdates().add(blockData);
                    byte[] skyData = lightData.getSkyLight(i);
                    if (skyData != null)
                        lightPacket.getSkyLightUpdates().add(skyData);
                }

                PendingLightData.setInstance(packet.getX(), packet.getZ(), null);

                onLightUpdate(lightPacket);
            }

            if (packet.isFullChunk())
                PendingChunkDataPackets.push(packet);
        }
    }

    @Inject(method = "onChunkData", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;addEntitiesToChunk(Lnet/minecraft/world/chunk/WorldChunk;)V"))
    private void onChunkDataSuccess(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_13_2) {
            PendingChunkDataPackets.pop();
        }
    }

    @Inject(method = "onChunkRenderDistanceCenter", at = @At("RETURN"))
    private void onOnChunkRenderDistanceCenter(ChunkRenderDistanceCenterS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_13_2) {
            if (packet.getChunkX() != lastCenterX || packet.getChunkZ() != lastCenterZ) {
                lastCenterX = packet.getChunkX();
                lastCenterZ = packet.getChunkZ();
                assert MinecraftClient.getInstance().getNetworkHandler() != null;
                PendingChunkDataPackets.processPackets(MinecraftClient.getInstance().getNetworkHandler()::onChunkData);
            }
        }
    }

    @Inject(method = "onCustomPayload", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void onOnCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_13_2) {
            Identifier channel = packet.getChannel();
            if (Protocol_1_13_2.CUSTOM_PAYLOAD_TRADE_LIST.equals(channel)) {
                PacketByteBuf buf = packet.getData();
                int syncId = buf.readInt();
                TraderOfferList trades = new TraderOfferList();
                int tradeCount = buf.readUnsignedByte();
                for (int i = 0; i < tradeCount; i++) {
                    ItemStack buy = buf.readItemStack();
                    ItemStack sell = buf.readItemStack();
                    boolean hasSecondItem = buf.readBoolean();
                    ItemStack secondBuy = hasSecondItem ? buf.readItemStack() : ItemStack.EMPTY;
                    boolean locked = buf.readBoolean();
                    int tradeUses = buf.readInt();
                    int maxTradeUses = buf.readInt();
                    TradeOffer trade = new TradeOffer(buy, secondBuy, sell, tradeUses, maxTradeUses, 0, 1);
                    if (locked)
                        trade.clearUses();
                    trades.add(trade);
                }
                onSetTradeOffers(new SetTradeOffersS2CPacket(syncId, trades, 5, 0, false, false));
                ci.cancel();
            } else if (Protocol_1_13_2.CUSTOM_PAYLOAD_OPEN_BOOK.equals(channel)) {
                OpenWrittenBookS2CPacket openBookPacket = new OpenWrittenBookS2CPacket();
                try {
                    openBookPacket.read(packet.getData());
                } catch (IOException e) {
                    LOGGER.error("Failed to read open book packet", e);
                }
                onOpenWrittenBook(openBookPacket);
                ci.cancel();
            }
        }
    }

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_13_2) {
            onDifficulty(new DifficultyS2CPacket(PendingDifficulty.getPendingDifficulty(), false));
        }
    }

    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    private void onOnPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_13_2) {
            onDifficulty(new DifficultyS2CPacket(PendingDifficulty.getPendingDifficulty(), false));
        }
    }

    @Inject(method = "onEntitySpawn", at = @At("TAIL"))
    private void onOnEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_13_2) {
            if (packet.getEntityTypeId() == EntityType.ITEM
                    || packet.getEntityTypeId() == EntityType.ARROW
                    || packet.getEntityTypeId() == EntityType.SPECTRAL_ARROW
                    || packet.getEntityTypeId() == EntityType.TRIDENT) {
                onVelocityUpdate(new EntityVelocityUpdateS2CPacket(packet.getId(),
                        new Vec3d(packet.getVelocityX(), packet.getVelocityY(), packet.getVelocityZ())));
            }
        }
    }

    @Inject(method = "onPlayerPositionLook", at = @At("TAIL"))
    private void onOnPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_13_2) {
            Protocol_1_13_2.updateCameraPosition();
        }
    }

}
