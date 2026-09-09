package net.jfrx.slashblade.maidnativepower.gametest;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.jfrx.slashblade.maidnativepower.task.TaskSlashBlade;
import net.jfrx.slashblade.maidnativepower.client.renderer.GeoLayerMaidBladeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/** Runs only with Gradle's runClientSmoke; excluded from the release jar. */
@EventBusSubscriber(modid = NativePowerOfMaid.MODID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static boolean prepared;
    private static int ticks;
    private static int bedrockFrames;
    private static int geckoFrames;
    private static int loadingTicks;
    private static int currentRenderer;
    private static int bedrockBlades;
    private static int geckoBlades;

    @SubscribeEvent
    public static void beforeRender(RenderLivingEvent.Pre<?, ?> event) {
        if (Boolean.getBoolean("native_power_of_maid.clientSmoke") && event.getEntity() instanceof EntityMaid) {
            currentRenderer = event.getRenderer() instanceof GeckoEntityMaidRenderer<?> ? 2 : 1;
        }
    }

    @SubscribeEvent
    public static void renderBlade(RenderOverrideEvent event) {
        if ("blade".equals(event.getTarget()) && !event.isCanceled()) {
            if (currentRenderer == 1) bedrockBlades++;
            if (currentRenderer == 2) geckoBlades++;
        }
    }

    @SubscribeEvent
    public static void afterRender(RenderLivingEvent.Post<?, ?> event) {
        if (Boolean.getBoolean("native_power_of_maid.clientSmoke") && event.getEntity() instanceof EntityMaid maid) {
            if (ticks > 80 && !TaskSlashBlade.UID.equals(maid.getTask().getUid())) {
                throw new IllegalStateException("Maid task failed to synchronize: " + maid.getTask().getUid());
            }
            if (ticks == 100) {
                var rank = maid.getData(CapabilityConcentrationRank.RANK_POINT);
                NativePowerOfMaid.LOGGER.info("Smoke maid model={}, blade={}, rank={}", maid.getModelId(), maid.getMainHandItem(), rank.getRank(maid.level().getGameTime()));
            }
            if (event.getRenderer() instanceof GeckoEntityMaidRenderer<?> gecko) {
                if (gecko.getLayerRenderers().stream().noneMatch(layer -> layer instanceof GeoLayerMaidBladeRenderer<?, ?>)) {
                    throw new IllegalStateException("Animated Gecko blade layer was not registered");
                }
                geckoFrames++;
            } else {
                bedrockFrames++;
            }
            currentRenderer = 0;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("native_power_of_maid.clientSmoke")) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) {
            if (++loadingTicks % 100 == 0) {
                NativePowerOfMaid.LOGGER.info("Client smoke loading screen: {}", client.screen == null ? "none" : client.screen.getClass().getSimpleName());
            }
            if (loadingTicks > 2400) {
                throw new IllegalStateException("Client smoke world did not load within two minutes");
            }
            return;
        }
        client.options.pauseOnLostFocus = false;
        if (!prepared) {
            prepared = true;
            var playerId = client.player.getUUID();
            client.getSingleplayerServer().execute(() -> {
                ServerPlayer player = client.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                if (player == null) {
                    throw new IllegalStateException("Smoke test player was not created");
                }
                var level = player.serverLevel();
                for (int x = -8; x <= 8; x++) {
                    for (int z = -8; z <= 8; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 63, z), Blocks.STONE.defaultBlockState());
                    }
                }
                level.setDayTime(6000);
                player.setGameMode(GameType.CREATIVE);
                player.teleportTo(level, 1, 64, 6, 180, 10);
                String[] models = {"touhou_little_maid:hakurei_reimu", "geckolib:winefox"};
                for (int i = 0; i < models.length; i++) {
                    EntityMaid maid = new EntityMaid(level);
                    maid.setPos(i * 2, 64, 0);
                    maid.setNoAi(true);
                    maid.setModelId(models[i]);
                    maid.setItemSlot(EquipmentSlot.MAINHAND, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
                    maid.setTask(new TaskSlashBlade());
                    maid.getData(CapabilityConcentrationRank.RANK_POINT).setRawRankPoint(2400);
                    maid.getData(CapabilityConcentrationRank.RANK_POINT).setLastUpdte(level.getGameTime());
                    level.addFreshEntity(maid);
                }
            });
        }
        if (++ticks == 160) {
            if (bedrockFrames < 3 || geckoFrames < 3) {
                throw new IllegalStateException("Missing maid renders: Bedrock=" + bedrockFrames + ", Gecko=" + geckoFrames);
            }
            if (bedrockBlades < 3 || geckoBlades < 3) {
                throw new IllegalStateException("Missing blade renders: Bedrock=" + bedrockBlades + ", Gecko=" + geckoBlades);
            }
            Screenshot.grab(client.gameDirectory, "native-power-client-smoke.png", client.getMainRenderTarget(),
                    message -> NativePowerOfMaid.LOGGER.info("Client smoke screenshot: {}", message.getString()));
            NativePowerOfMaid.LOGGER.info("CLIENT SMOKE PASSED: Bedrock={} frames/{} blades, Gecko={} frames/{} blades", bedrockFrames, bedrockBlades, geckoFrames, geckoBlades);
        }
        if (ticks == 180) {
            client.stop();
        }
    }
}
