package com.mundodetronos2.events;

import com.mundodetronos2.config.ConfigManager;
import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import com.mundodetronos2.role.AltarManager;
import com.mundodetronos2.role.PortalsManager;
import com.mundodetronos2.role.PlayerRole;
import com.mundodetronos2.role.PlayerRoleData;
import com.mundodetronos2.role.RoleManager;
import com.mundodetronos2.progression.ProgressionManager;
import com.mundodetronos2.throne.ThroneData;
import com.mundodetronos2.throne.ThroneManager;
import com.mundodetronos2.throne.ThroneState;
import com.mundodetronos2.time.TimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "mundodetronos2", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GameEventHandler {

    // ------------------ SERVER EVENTS ------------------

    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        // Cargar persistencia al iniciar el servidor
        ConfigManager.load();
        RealmManager.init();
        ThroneManager.init();
        RoleManager.init();
        ProgressionManager.init();
        com.mundodetronos2.dialogue.NpcDialogueManager.init();
        AltarManager.load();
        TimeManager.init();
        PortalsManager.load();
        com.mundodetronos2.role.EquipmentManager.init();
        com.mundodetronos2.npc.NpcRegistryManager.init();
        com.mundodetronos2.tutorial.TutorialManager.init();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Guardar persistencia de forma síncrona/forzada al apagar
        RealmManager.save(true);
        RoleManager.shutdown();
        ProgressionManager.shutdown();
        TimeManager.shutdown();
        SaveManagerShim.shutdown();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Ticker de cargas de asalto (cuenta regresiva y explosión)
            com.mundodetronos2.throne.ThroneAttackManager.tick();

            // Se ejecuta una vez por tick del servidor.
            // Para optimizar al máximo, solo chequeamos los cooldowns de reparación una vez por segundo (cada 20 ticks)
            if (ServerTickCounter.tickCount++ % 20 == 0) {
                ThroneManager.tick();
                playRepairTickSounds();

                // Decrementar tiempo de juego de forma optimizada
                TimeManager.tick(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());

                // Decrementar espectador fantasma de muerte
                DeathSpectatorManager.tick(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            if (event.player instanceof ServerPlayer sp) {
                // Check Guardia del Rey exploration checkpoints
                com.mundodetronos2.tutorial.TutorialManager.checkGuardiaExploration(sp);

                // Push back non-member players trying to cross unbroken wall territory borders
                if (!sp.hasPermissions(2)) {
                    BlockPos pos = sp.blockPosition();
                    String dim = sp.level().dimension().location().toString();
                    for (ThroneData t : ThroneManager.getThronesMap().values()) {
                        if (t.getDimension().equals(dim) && t.getPos() != null) {
                            BlockPos tPos = t.getPos();
                            int radius = t.getProtectionRadius();
                            if (Math.abs(tPos.getX() - pos.getX()) <= radius && Math.abs(tPos.getZ() - pos.getZ()) <= radius) {
                                RealmData r = RealmManager.getRealm(t.getRealmId());
                                if (r != null && !r.getMembers().contains(sp.getUUID())) {
                                    // Check if closest wall is unbroken
                                    if (ThroneManager.isWallBlock(pos) || ThroneManager.isWallBlock(pos.below())) {
                                        sp.setDeltaMovement(sp.getDeltaMovement().x * -1.5D, 0.2D, sp.getDeltaMovement().z * -1.5D);
                                        sp.hurtMarked = true;
                                        com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Territorio enemigo fortificado. Debes abrir una brecha en la muralla.");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Prevent player from falling into the void in the Goddess dimension
            if (event.player.level().dimension().location().toString().equals("mundodetronos2:role_dimension")) {
                if (event.player.getY() < 50.0) {
                    event.player.teleportTo(0.5, 64.0, 0.5);
                    event.player.setDeltaMovement(0, 0, 0);
                    event.player.resetFallDistance();
                }
            }

            // Prevent custom role armor and initial kit armor from breaking, and move almost broken armor to inventory
            for (int i = 0; i < event.player.getInventory().armor.size(); i++) {
                net.minecraft.world.item.ItemStack armor = event.player.getInventory().armor.get(i);
                if (armor != null && !armor.isEmpty() && armor.hasTag()) {
                    boolean isRoleArmor = armor.getTag().getBoolean("mundodetronos2:role_armor");
                    boolean isInitialArmor = "any".equalsIgnoreCase(armor.getTag().getString("AuthorizedRole")) && armor.getTag().getString("RoleItemID").startsWith("kit_inicial_");

                    if (isRoleArmor || isInitialArmor) {
                        int maxDamage = armor.getMaxDamage();
                        if (armor.getDamageValue() >= maxDamage - 1) {
                            armor.setDamageValue(maxDamage - 1);

                            // Mover al inventario principal si es posible para que la reparen
                            if (event.player.getInventory().add(armor.copy())) {
                                // Eliminar de la ranura de armadura equipada
                                event.player.getInventory().armor.set(i, net.minecraft.world.item.ItemStack.EMPTY);
                                if (event.player instanceof ServerPlayer sp) {
                                    com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Tu armadura está dañada y ha sido guardada en tu inventario para que la repares.");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String toDimension = event.getTo().location().toString();
            if (toDimension.equals("mundodetronos2:role_dimension")) {
                ServerLevel level = player.serverLevel();
                // Create a solid 7x7 platform of barrier blocks centered at 0, 63, 0
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        BlockPos bp = new BlockPos(x, 63, z);
                        if (level.isEmptyBlock(bp)) {
                            level.setBlockAndUpdate(bp, net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!player.level().isClientSide) {
                java.util.Iterator<net.minecraft.world.entity.item.ItemEntity> iter = event.getDrops().iterator();
                while (iter.hasNext()) {
                    net.minecraft.world.entity.item.ItemEntity entity = iter.next();
                    net.minecraft.world.item.ItemStack stack = entity.getItem();
                    if (stack.getItem() == com.mundodetronos2.init.ItemInit.ROLE_CARD.get()) {
                        iter.remove();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player oldPlayer = event.getOriginal();
            Player newPlayer = event.getEntity();
            for (net.minecraft.world.item.ItemStack stack : oldPlayer.getInventory().items) {
                if (stack.getItem() == com.mundodetronos2.init.ItemInit.ROLE_CARD.get()) {
                    newPlayer.getInventory().add(stack.copy());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            int remaining = TimeManager.getRemainingSeconds(sp.getUUID());
            if (remaining <= 0) {
                sp.connection.disconnect(Component.literal("Tu tiempo se terminó."));
            } else {
                // Sincronizar nivel de tutorial con el equipo al entrar
                com.mundodetronos2.tutorial.TutorialManager.syncPlayerLevelWithTeam(sp);
                // Sincronizar HUD inicial al conectar
                NetworkManager.syncHud(sp);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // Otorgar XP por derrotar Mobs al jugador atacante
        if (event.getSource().getEntity() instanceof ServerPlayer killer && !(event.getEntity() instanceof Player)) {
            if (ConfigManager.get().enableMobKillXp) {
                boolean isBoss = event.getEntity() instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                              || event.getEntity() instanceof net.minecraft.world.entity.boss.wither.WitherBoss;
                int xpAmount = isBoss ? ConfigManager.get().bossMobKillXp : ConfigManager.get().defaultMobKillXp;
                ProgressionManager.addXp(killer.getUUID(), xpAmount, killer);
            }
        }

        if (event.getEntity() instanceof ServerPlayer sp) {
            // Cancelar el evento de muerte real para que no muera físicamente
            event.setCanceled(true);

            // Iniciar espectador de muerte de 15 segundos donde murió
            DeathSpectatorManager.startPlayerDeathSpectating(sp);

            // Enviar alerta global de muerte y pérdida de puntos a todos los jugadores online
            for (ServerPlayer player : sp.getServer().getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(new NetworkManager.S2CShowDeathAlertPacket(sp.getUUID(), sp.getGameProfile().getName()), player);
            }

            RealmData realm = RealmManager.getPlayerRealm(sp.getUUID());
            if (realm != null) {
                // Deduct 5 points, ensuring it never goes below 0 (handled in RealmData)
                realm.setSharedPoints(realm.getSharedPoints() - 5);
                com.mundodetronos2.data.SaveManager.markDirty();
                RealmManager.save(false);

                // Sincronizar HUD a todos los integrantes online de este reino
                for (UUID memberId : realm.getMembers()) {
                    ServerPlayer member = sp.getServer().getPlayerList().getPlayer(memberId);
                    if (member != null) {
                        NetworkManager.syncHud(member);
                    }
                }
            }
        }
    }

    private static void playRepairTickSounds() {
        for (ThroneData throne : ThroneManager.getThronesMap().values()) {
            if (throne.getState() == ThroneState.REPAIRING) {
                // Sonido ocasional durante la reparación (15% de probabilidad cada segundo)
                if (Math.random() < 0.15) {
                    BlockPos p = throne.getPos();
                    sendThroneSound(p.getX(), p.getY(), p.getZ(), "repair_tick", throne.getDimension());
                }
            }
        }
    }

    public static void sendThroneSound(double x, double y, double z, String soundType, String dimensionStr) {
        NetworkManager.S2CPlayThroneHitSoundPacket pkt = new NetworkManager.S2CPlayThroneHitSoundPacket(x, y, z, soundType);
        // Enviar solo a jugadores cercanos en la misma dimensión
        for (Player p : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (p instanceof ServerPlayer sp) {
                if (sp.level().dimension().location().toString().equals(dimensionStr)) {
                    double distSq = sp.distanceToSqr(x, y, z);
                    if (distSq < 1024) { // 32 bloques
                        NetworkManager.sendToPlayer(pkt, sp);
                    }
                }
            }
        }
    }

    // --- CONTROLADORES DE SEGURIDAD PARA EL TRONO INDESTRUCTIBLE Y DAÑO POR PROYECTILES ---

    public static boolean isPosProtectedByThrone(BlockPos pos, String dimension, Player player) {
        if (player.hasPermissions(2)) {
            return false; // OPs/Admins can always bypass
        }

        // Si el evento global de asalto está ACTIVO, la protección normal está desactivada!
        if (ThroneManager.isGlobalEventActive()) {
            return false;
        }

        for (ThroneData t : ThroneManager.getThronesMap().values()) {
            if (t.getDimension().equals(dimension) && t.getPos() != null) {
                BlockPos tPos = t.getPos();
                int radius = t.getProtectionRadius();
                if (Math.abs(tPos.getX() - pos.getX()) <= radius && Math.abs(tPos.getZ() - pos.getZ()) <= radius) {
                    // El bloque está dentro de una zona de protección dinámica alrededor del trono!
                    // Verificar si el jugador es miembro del equipo propietario
                    com.mundodetronos2.player.PlayerRealmData prd = RealmManager.getPlayerRealmData(player.getUUID());
                    if (prd == null || !prd.getRealmId().equals(t.getRealmId())) {
                        return true; // Está protegido contra este jugador!
                    }
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onBlockBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide()) {
            BlockPos pos = event.getPos();
            if (ThroneManager.getThroneAt(pos) != null) {
                // Cancelar la rotura física del bloque del trono por completo, incluso para OPs
                event.setCanceled(true);
                if (event.getPlayer() instanceof ServerPlayer sp) {
                    NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§c[Mundo de Tronos] ¡El bloque del trono es indestructible! Usa /tronos remover para quitarlo.", true), sp);
                }
                return;
            }

            if (ThroneManager.isWallBlock(pos)) {
                event.setCanceled(true);
                if (event.getPlayer() instanceof ServerPlayer sp) {
                    NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§c[Mundo de Tronos] ¡Las murallas de la fortaleza son indestructibles! Usa Cargas de Asalto para destruirlas.", true), sp);
                }
                return;
            }

            // Protección de zona
            Player player = event.getPlayer();
            String dim = player.level().dimension().location().toString();
            if (isPosProtectedByThrone(pos, dim, player)) {
                event.setCanceled(true);
                if (player instanceof ServerPlayer sp) {
                    com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Esta zona está protegida por una base enemiga.");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide()) {
            event.getAffectedBlocks().removeIf(pos -> ThroneManager.getThroneAt(pos) != null || ThroneManager.isWallBlock(pos));
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Player player) {
            BlockPos pos = event.getPos();
            String dim = player.level().dimension().location().toString();

            if (player instanceof ServerPlayer sp) {
                net.minecraft.world.item.ItemStack heldItem = sp.getMainHandItem();
                if (heldItem.isEmpty() || !heldItem.hasTag() || !heldItem.getTag().getBoolean("mundodetronos2:throne_item")) {
                    heldItem = sp.getOffhandItem();
                }
                if (!heldItem.isEmpty() && heldItem.hasTag() && heldItem.getTag().getBoolean("mundodetronos2:throne_item")) {
                    // Es la colocación de un trono oficial
                    RealmData realm = RealmManager.getPlayerRealm(sp.getUUID());
                    if (realm == null) {
                        event.setCanceled(true);
                        com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ No perteneces a ningún equipo.");
                        return;
                    }
                    if (!realm.getOwnerId().equals(sp.getUUID())) {
                        event.setCanceled(true);
                        com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Solo el líder del Team puede colocar su Trono.");
                        return;
                    }
                    if (realm.getThroneId() != null) {
                        event.setCanceled(true);
                        com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Tu equipo ya tiene un trono registrado.");
                        return;
                    }

                    // Registrar el Trono en esta posición con protección 150x150 (radio 75)
                    ThroneData throne = ThroneManager.registerThrone(realm.getId(), pos, dim, 3);
                    if (throne != null) {
                        throne.setProtectionRadius(75);
                        com.mundodetronos2.throne.FortressWallManager.buildOrUpdateWall(sp.serverLevel(), throne);
                        com.mundodetronos2.throne.DefenderManager.spawnInitialDefenders(sp.serverLevel(), throne);
                        com.mundodetronos2.network.MessageManager.actionBar(sp, "§a✔ ¡El Trono ha sido colocado, fortaleza fortificada y defensores invocados!");
                        sp.serverLevel().sendParticles(
                            net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D,
                            30, 0.5D, 0.5D, 0.5D, 0.1D
                        );
                        // Forzar sincronización HUD de inmediato
                        NetworkManager.syncHud(sp);
                    } else {
                        event.setCanceled(true);
                        com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Error al registrar el trono.");
                    }
                    return;
                }
            }

            if (isPosProtectedByThrone(pos, dim, player)) {
                event.setCanceled(true);
                if (player instanceof ServerPlayer sp) {
                    com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Esta zona está protegida por una base enemiga.");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEnderPearlTeleport(net.minecraftforge.event.entity.EntityTeleportEvent.EnderPearl event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer sp) {
            if (sp.hasPermissions(2)) return;

            BlockPos targetPos = BlockPos.containing(event.getTargetX(), event.getTargetY(), event.getTargetZ());
            String dim = sp.level().dimension().location().toString();

            for (ThroneData t : ThroneManager.getThronesMap().values()) {
                if (t.getDimension().equals(dim) && t.getPos() != null) {
                    BlockPos tPos = t.getPos();
                    int radius = t.getProtectionRadius();
                    if (Math.abs(tPos.getX() - targetPos.getX()) <= radius && Math.abs(tPos.getZ() - targetPos.getZ()) <= radius) {
                        RealmData r = RealmManager.getRealm(t.getRealmId());
                        if (r != null && !r.getMembers().contains(sp.getUUID())) {
                            event.setCanceled(true);
                            com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Prohibido usar Ender Pearls para atravesar la muralla enemiga.");
                            return;
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide() && event.getRayTraceResult().getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) event.getRayTraceResult();
            BlockPos pos = blockHit.getBlockPos();
            ThroneData throne = ThroneManager.getThroneAt(pos);
            if (throne != null) {
                // El proyectil impactó el trono!
                event.setCanceled(true); // Cancelar impacto vanilla
                if (event.getProjectile().getOwner() instanceof ServerPlayer sp) {
                    com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ El trono solo puede ser dañado con una Carga de Asalto.");
                }
            }
        }
    }

    public static void reduceThroneLife(net.minecraft.server.MinecraftServer server, UUID attackerId, String attackerName, ThroneData throne, BlockPos pos) {
        RealmData realm = RealmManager.getRealm(throne.getRealmId());
        if (realm != null) {
            int lives = realm.getCurrentLives();
            int oldLives = lives;
            if (lives > 0) {
                lives--;
                realm.setCurrentLives(lives);
            }
            int newLives = lives;

            // 1. Enviar una línea de anuncio global usando Base / Equipo
            String globalMsg = "§6[Mundo de Tronos] §c¡La base '" + realm.getName() + "' ha perdido una vida del trono a manos de " + attackerName + "! (Vidas: " + oldLives + " → " + newLives + ")";
            broadcastMessage(globalMsg);

            // 2. Enviar el paquete de la alerta lateral y reproducir el sonido de forma controlada
            NetworkManager.S2CThroneLifeLossAlertPacket alertPkt = new NetworkManager.S2CThroneLifeLossAlertPacket(
                realm.getName(), attackerId, attackerName, oldLives, newLives
            );

            // Sound and alert targets
            for (ServerPlayer srvPlayer : server.getPlayerList().getPlayers()) {
                boolean isMember = realm.getMembers().contains(srvPlayer.getUUID());
                boolean isAttacker = srvPlayer.getUUID().equals(attackerId);
                boolean isClose = srvPlayer.level().dimension().location().toString().equals(throne.getDimension())
                                  && srvPlayer.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4096; // 64 bloques

                if (isMember || isAttacker || isClose) {
                    // Enviar la alerta lateral
                    NetworkManager.sendToPlayer(alertPkt, srvPlayer);

                    // Sonido épico controlado
                    srvPlayer.playNotifySound(net.minecraft.sounds.SoundEvents.ENDER_DRAGON_DEATH, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.8F);
                    srvPlayer.playNotifySound(net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.9F);
                }
            }

            throne.setState(ThroneState.REPAIRING);
            long cooldownMinutes = ConfigManager.get().throneCooldownMinutes;
            throne.setCooldownEndsAt(System.currentTimeMillis() + (cooldownMinutes * 60 * 1000));
            throne.setHealth(0);

            sendThroneSound(pos.getX(), pos.getY(), pos.getZ(), "repair_start", throne.getDimension());

            for (UUID memberId : realm.getMembers()) {
                ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                if (member != null) {
                    NetworkManager.syncHud(member);
                }
            }
        }
    }

    private static void applyDamageToThroneFromPlayer(ServerPlayer sp, ThroneData throne, BlockPos pos) {
        UUID playerId = sp.getUUID();
        RealmData playerRealm = RealmManager.getPlayerRealm(playerId);

        // Validaciones
        if (playerRealm != null && playerRealm.getId().equals(throne.getRealmId())) {
            NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§cNo puedes dañar el trono de tu propio reino.", true), sp);
            return;
        }

        if (!ThroneManager.isGlobalEventActive()) {
            NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§eEl trono está protegido. No hay ningún evento activo.", true), sp);
            return;
        }

        if (throne.getState() == ThroneState.REPAIRING) {
            NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§eEl trono se está reparando actualmente.", true), sp);
            return;
        }

        int currentHp = throne.getHealth();
        if (currentHp > 0) {
            currentHp--;
            throne.setHealth(currentHp);
            throne.setLastAttacker(playerId);
            com.mundodetronos2.data.SaveManager.markDirty();

            RealmData throneRealm = RealmManager.getRealm(throne.getRealmId());
            String realmName = throneRealm != null ? throneRealm.getName() : "Desconocido";
            NetworkManager.sendToPlayer(new NetworkManager.S2CSyncThroneDataPacket(realmName, currentHp, throne.getMaxHealth(), throne.getState().name()), sp);

            sendThroneSound(pos.getX(), pos.getY(), pos.getZ(), "hit", throne.getDimension());

            if (currentHp <= 0) {
                reduceThroneLife(sp.getServer(), sp.getUUID(), sp.getGameProfile().getName(), throne, pos);
            }
            RealmManager.save(false);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level world = event.getLevel();
        if (world.isClientSide) return;

        BlockPos pos = event.getPos();
        ThroneData throne = ThroneManager.getThroneAt(pos);
        if (throne == null) return;

        // Cancelar de inmediato para evitar que el bloque sea roto físicamente
        event.setCanceled(true);

        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer sp)) return;

        // Mandar el mensaje por action bar
        com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ El trono solo puede ser dañado con una Carga de Asalto.");
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level world = event.getLevel();
        if (world.isClientSide) return;

        // Solo procesar la mano principal para evitar doble ejecución (MAIN_HAND y OFF_HAND)
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        // Validar equipamiento no autorizado en clic derecho a bloques
        Player player = event.getEntity();
        if (player instanceof ServerPlayer sp) {
            net.minecraft.world.item.ItemStack held = sp.getItemInHand(event.getHand());
            if (com.mundodetronos2.role.EquipmentRestrictions.isRestrictedItem(held)) {
                PlayerRoleData roleData = RoleManager.getPlayerRoleData(sp.getUUID());
                String role = roleData.isHasRole() ? roleData.getRole().name() : "none";
                if (!com.mundodetronos2.role.EquipmentRestrictions.isItemAuthorized(held, role)) {
                    event.setCanceled(true);
                    NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§cEse objeto no está autorizado para tu rol.", true), sp);
                    return;
                }
            }
        }

        BlockPos pos = event.getPos();
        String dimension = world.dimension().location().toString();

        ThroneData clickedThrone = ThroneManager.getThroneAt(pos);
        if (clickedThrone != null && player instanceof ServerPlayer sp) {
            net.minecraft.world.item.ItemStack held = sp.getItemInHand(event.getHand());
            if (!held.isEmpty() && held.hasTag() && held.getTag().getBoolean("mundodetronos2:throne_relocation_key")) {
                RealmData realm = RealmManager.getPlayerRealm(sp.getUUID());
                if (realm != null && realm.getId().equals(clickedThrone.getRealmId())) {
                    if (realm.getOwnerId().equals(sp.getUUID())) {
                        event.setCanceled(true);
                        ThroneManager.removeThroneByRealm(clickedThrone.getRealmId());
                        realm.setThroneId(null);
                        world.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        held.shrink(1);

                        net.minecraft.world.item.ItemStack throneStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_BLOCK);
                        throneStack.getOrCreateTag().putBoolean("mundodetronos2:throne_item", true);
                        throneStack.setHoverName(Component.literal("§6§lTrono Pendiente de Colocación"));
                        sp.getInventory().add(throneStack);

                        com.mundodetronos2.network.MessageManager.actionBar(sp, "§a✔ Trono reubicado. Terreno liberado. Coloca tu nuevo trono.");
                        NetworkManager.syncHud(sp);
                        return;
                    } else {
                        event.setCanceled(true);
                        com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Solo el líder del equipo puede reubicar el trono.");
                        return;
                    }
                }
            }
        }

        // Protección de zona para clic derecho (cofres, puertas, etc.)
        boolean isSpecBlock = AltarManager.isAltar(pos, dimension) || PortalsManager.isPortal(pos, dimension) || world.getBlockState(pos).is(com.mundodetronos2.init.BlockInit.CARGA_ASALTO_BLOCK.get());
        if (!isSpecBlock && isPosProtectedByThrone(pos, dimension, player)) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer sp) {
                com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Esta zona está protegida por una base enemiga.");
            }
            return;
        }

        if (AltarManager.isAltar(pos, dimension)) {
            // Cancelar el evento para evitar que el huevo de dragón se teletransporte por clic
            event.setCanceled(true);

            if (player instanceof ServerPlayer sp) {
                PlayerRoleData data = RoleManager.getPlayerRoleData(sp.getUUID());
                if (dimension.equalsIgnoreCase("mundodetronos2:role_dimension")) {
                    // Abrir menú de interacción del Altar (Selección/Información o Salida)
                    NetworkManager.sendToPlayer(new NetworkManager.S2COpenAltarOptionPacket(data.isHasRole()), sp);
                } else {
                    if (data.isHasRole()) {
                        // Ya tiene rol: sincronizar datos y abrir carnet
                        RealmData realm = RealmManager.getPlayerRealm(sp.getUUID());
                        String rName = realm != null ? realm.getName() : "Ninguno";
                        String rRole = realm != null ? (realm.getOwnerId().equals(sp.getUUID()) ? "Líder" : "Miembro") : "N/A";

                        com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(sp.getUUID());
                        int currentXp = pData.getXp();
                        int neededXp = com.mundodetronos2.progression.LevelSystem.getXpNeeded(pData.getLevel());

                        NetworkManager.S2CRoleCardDataPacket pkt = new NetworkManager.S2CRoleCardDataPacket(
                                sp.getUUID(),
                                sp.getGameProfile().getName(),
                                data.getRole().name(),
                                data.getLevel(),
                                rName,
                                rRole,
                                currentXp,
                                neededXp
                        );
                        NetworkManager.sendToPlayer(pkt, sp);
                    } else {
                        // No tiene rol: abrir pantalla de selección
                        NetworkManager.sendToPlayer(new NetworkManager.S2COpenRoleSelectionPacket(), sp);
                    }
                }
            }
        } else if (PortalsManager.isPortal(pos, dimension)) {
            // Cancelar evento para abrir la interfaz del portal de la Diosa María
            event.setCanceled(true);

            if (player instanceof ServerPlayer sp) {
                PlayerRoleData rData = RoleManager.getPlayerRoleData(sp.getUUID());
                if (rData.isHasRole() && rData.getRole() != PlayerRole.NONE) {
                    // Teletransportar de inmediato gratis e instantáneo con el carnet sin requerir ofrenda
                    try {
                        ResourceLocation dimRl = new ResourceLocation("mundodetronos2", "role_dimension");
                        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimRl);
                        ServerLevel level = sp.getServer().getLevel(dimKey);
                        if (level != null) {
                            sp.teleportTo(level, 0.5D, 64.0D, 0.5D, 0.0F, 0.0F);
                            NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§a¡Has regresado al templo de la Diosa María usando tu carnet!", false), sp);
                        } else {
                            NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§cLa dimensión de roles no se encuentra cargada en el servidor.", true), sp);
                        }
                    } catch (Exception e) {
                        NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§cError al cruzar el portal: " + e.getMessage(), true), sp);
                    }
                } else {
                    // Abrir la pantalla del portal de ofrenda en el cliente
                    NetworkManager.sendToPlayer(new NetworkManager.S2COpenGoddessPortalPacket(), sp);
                }
            }
        }
    }

    // --- NUEVOS CONTROLADORES DE BLOQUEO DE EQUIPAMIENTO POR ROL (EVENT-DRIVEN, SIN TICKS PESADOS) ---

    @SubscribeEvent
    public static void onPlayerAttack(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && player instanceof ServerPlayer sp) {
            if (event.getTarget() instanceof net.minecraft.world.entity.animal.IronGolem golem) {
                com.mundodetronos2.tutorial.TutorialManager.onGolemHit(sp, golem);
            }

            net.minecraft.world.item.ItemStack held = sp.getMainHandItem();
            if (com.mundodetronos2.role.EquipmentRestrictions.isRestrictedItem(held)) {
                PlayerRoleData roleData = RoleManager.getPlayerRoleData(sp.getUUID());
                String role = roleData.isHasRole() ? roleData.getRole().name() : "none";
                if (!com.mundodetronos2.role.EquipmentRestrictions.isItemAuthorized(held, role)) {
                    event.setCanceled(true);
                    NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§cEse objeto no está autorizado para tu rol.", true), sp);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && player instanceof ServerPlayer sp) {
            net.minecraft.world.item.ItemStack held = event.getItemStack();
            if (com.mundodetronos2.role.EquipmentRestrictions.isRestrictedItem(held)) {
                PlayerRoleData roleData = RoleManager.getPlayerRoleData(sp.getUUID());
                String role = roleData.isHasRole() ? roleData.getRole().name() : "none";
                if (!com.mundodetronos2.role.EquipmentRestrictions.isItemAuthorized(held, role)) {
                    event.setCanceled(true);
                    NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§cEse objeto no está autorizado para tu rol.", true), sp);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (event.getSlot().getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
                net.minecraft.world.item.ItemStack toItem = event.getTo();
                if (com.mundodetronos2.role.EquipmentRestrictions.isRestrictedItem(toItem)) {
                    PlayerRoleData roleData = RoleManager.getPlayerRoleData(sp.getUUID());
                    String role = roleData.isHasRole() ? roleData.getRole().name() : "none";
                    if (!com.mundodetronos2.role.EquipmentRestrictions.isItemAuthorized(toItem, role)) {
                        // Es una armadura no autorizada equipada, removerla de inmediato
                        sp.setItemSlot(event.getSlot(), net.minecraft.world.item.ItemStack.EMPTY);
                        if (!sp.getInventory().add(toItem.copy())) {
                            sp.drop(toItem.copy(), false);
                        }
                        NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("§cEse objeto no está autorizado para tu rol.", true), sp);
                    }
                }
            }
        }
    }

    public static void broadcastMessage(String msg) {
        Component comp = Component.literal(msg);
        for (Player p : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            p.sendSystemMessage(comp);
        }
    }

    // Clase auxiliar para ticks del servidor
    private static class ServerTickCounter {
        static int tickCount = 0;
    }

    private static class SaveManagerShim {
        static void shutdown() {
            com.mundodetronos2.data.SaveManager.shutdown();
        }
    }
}
