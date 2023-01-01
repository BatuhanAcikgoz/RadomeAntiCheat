package com.elikill58.negativity.sponge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.PotionEffectData;
import org.spongepowered.api.data.manipulator.mutable.entity.FallDistanceData;
import org.spongepowered.api.data.manipulator.mutable.entity.FlyingData;
import org.spongepowered.api.data.manipulator.mutable.entity.HealthData;
import org.spongepowered.api.data.manipulator.mutable.entity.VelocityData;
import org.spongepowered.api.data.manipulator.mutable.item.EnchantmentData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.elikill58.negativity.sponge.listeners.PlayerCheatEvent;
import com.elikill58.negativity.sponge.listeners.PlayerPacketsClearEvent;
import com.elikill58.negativity.sponge.precogs.NegativityBypassTicket;
import com.elikill58.negativity.sponge.protocols.ForceFieldProtocol;
import com.elikill58.negativity.sponge.support.ViaVersionSupport;
import com.elikill58.negativity.sponge.utils.Utils;
import com.elikill58.negativity.universal.Cheat;
import com.elikill58.negativity.universal.Cheat.CheatHover;
import com.elikill58.negativity.universal.CheatKeys;
import com.elikill58.negativity.universal.FlyingReason;
import com.elikill58.negativity.universal.NegativityAccount;
import com.elikill58.negativity.universal.NegativityPlayer;
import com.elikill58.negativity.universal.ReportType;
import com.elikill58.negativity.universal.Version;
import com.elikill58.negativity.universal.adapter.Adapter;
import com.elikill58.negativity.universal.permissions.Perm;
import com.elikill58.negativity.universal.utils.UniversalUtils;
import com.flowpowered.math.vector.Vector3d;

public class SpongeNegativityPlayer extends NegativityPlayer {

	private static final Map<UUID, SpongeNegativityPlayer> PLAYERS_CACHE = new HashMap<>();

	public static ArrayList<Player> INJECTED = new ArrayList<>();
	public HashMap<String, String> MODS = new HashMap<>();
	public ArrayList<PotionEffect> POTION_EFFECTS = new ArrayList<>();
	public ArrayList<FakePlayer> FAKE_PLAYER = new ArrayList<>();
	public Map<Cheat, List<PlayerCheatEvent.Alert>> pendingAlerts = new HashMap<>();
	public HashMap<String, Double> contentDouble = new HashMap<>();
	public HashMap<String, Boolean> contentBoolean = new HashMap<>();
	public ArrayList<Double> flyMoveAmount = new ArrayList<>();
	private Player p = null;
	// Packets
	public int FLYING = 0, MAX_FLYING = 0, POSITION_LOOK = 0, KEEP_ALIVE = 0, POSITION = 0, BLOCK_PLACE = 0, BLOCK_DIG = 0, ARM = 0, USE_ENTITY = 0, ENTITY_ACTION = 0, ALL = 0;
	// warns & other
	public int LAST_CLICK = 0, ACTUAL_CLICK = 0, SEC_ACTIVE = 0;
	// setBack
	public int NO_FALL_DAMAGE = 0, BYPASS_SPEED = 0, SPEED_NB = 0, SPIDER_SAME_DIST = 0;
	public double lastYDiff = -3.142654;
	public long TIME_OTHER_KEEP_ALIVE = 0, TIME_INVINCIBILITY = 0, LAST_SHOT_BOW = 0, LAST_REGEN = 0, LAST_BLOCK_BREAK = 0, LAST_CLICK_INV = 0, LAST_BLOCK_PLACE = 0, TIME_REPORT = 0;
	public String LAST_OTHER_KEEP_ALIVE;
	public boolean IS_LAST_SEC_SNEAK = false, isFreeze = false, isUsingSlimeBlock = false, isJumpingWithBlock = false, isOnLadders = false, lastClickInv = false;
	private boolean mustToBeSaved = false;
	public FlyingReason flyingReason = FlyingReason.REGEN;
	public ItemType eatMaterial = ItemTypes.AIR;
	private final List<String> proofs = new ArrayList<>();
	public boolean isInFight = false;
	public Task fightTask = null;
	public int fakePlayerTouched = 0;
	public long timeStartFakePlayer = 0;
	public Location<World> lastSpiderLoc = null;
	public boolean justDismounted = false;
	private final Version playerVersion;

	public SpongeNegativityPlayer(Player p) {
		super(p.getUniqueId(), p.getName());
		this.p = p;
		playerVersion = SpongeNegativity.viaVersionSupport ? ViaVersionSupport.getPlayerVersion(p) : Version.getVersion();
	}

	public SpongeNegativityPlayer(User p) {
		super(p.getUniqueId(), p.getName());
		playerVersion = Version.getVersion();
	}

	public void initFmlMods() {
		if (SpongeForgeSupport.isOnSpongeForge) {
			MODS = SpongeForgeSupport.getClientMods(p);
		} else {
			sendFmlPacket((byte) -2, (byte) 0);
			sendFmlPacket((byte) 0, (byte) 2, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
			sendFmlPacket((byte) 2, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
		}
	}

	private void sendFmlPacket(byte... data) {
		if (SpongeNegativity.fmlChannel == null)
			return;
		SpongeNegativity.fmlChannel.sendTo(p, (payload) -> {
			payload.writeBytes(data);
		});
	}

	public Player getPlayer() {
		return p;
	}

	public String getIP() {
		return getPlayer().getConnection().getAddress().getAddress().getHostAddress();
	}

	@Override
	public Version getPlayerVersion() {
		return playerVersion;
	}

	public boolean hasDetectionActive(Cheat c) {
		if (!c.isActive())
			return false;
		if (TIME_INVINCIBILITY > System.currentTimeMillis())
			return false;
		if (isInFight && c.isBlockedInFight())
			return false;
		// if(WorldRegionBypass.hasBypass(c, p.getLocation()))
		// return false;
		if (SpongeNegativity.hasBypass && (Perm.hasPerm(this, "bypass." + c.getKey().toLowerCase(Locale.ROOT)) || Perm.hasPerm(this, "bypass.all")))
			return false;
		if (hasBypassTicket(c))
			return false;
		Player p = getPlayer();
		return p == null || Utils.getPing(p) < c.getMaxAlertPing();
	}

	public void logProof(String msg) {
		proofs.add(msg);
	}

	public void saveData() {
		if (mustToBeSaved) {
			mustToBeSaved = false;
			Adapter.getAdapter().getAccountManager().save(getUUID());
		}
		if (!proofs.isEmpty()) {
			try {
				Path userDir = SpongeNegativity.getInstance().getDataFolder().resolve("user");
				Path proofDir = userDir.resolve("proof");
				Files.createDirectories(proofDir);
				Path proofFile = proofDir.resolve(p.getUniqueId() + ".txt");
				Files.write(proofFile, proofs, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				proofs.clear();
			} catch (IOException e) {
				SpongeNegativity.getInstance().getLogger().error("Unable to save proofs of player " + p.getName(), e);
			}
		}
	}

	public boolean hasBypassTicket(Cheat c) {
		if (!SpongeNegativity.hasPrecogs)
			return false;
		return NegativityBypassTicket.hasBypassTicket(c, p);
	}

	@Deprecated
	public void addWarn(Cheat c) {
		addWarn(c, 100);
	}

	public void addWarn(Cheat c, int reliability) {
		addWarn(c, 100, 1);
	}

	public void addWarn(Cheat c, int reliability, int amount) {
		if (System.currentTimeMillis() < TIME_INVINCIBILITY || c.getReliabilityAlert() > reliability)
			return;
		NegativityAccount account = getAccount();
		account.setWarnCount(c, account.getWarn(c) + amount);
		mustToBeSaved = true;
	}

	public void clearPackets() {
		PlayerPacketsClearEvent event = new PlayerPacketsClearEvent(p, this);
		Sponge.getEventManager().post(event);
		if (FLYING > MAX_FLYING)
			MAX_FLYING = FLYING;
		FLYING = 0;
		POSITION_LOOK = 0;
		KEEP_ALIVE = 0;
		POSITION = 0;
		BLOCK_PLACE = 0;
		BLOCK_DIG = 0;
		ARM = 0;
		USE_ENTITY = 0;
		ENTITY_ACTION = 0;
		ALL = 0;
	}

	public void startAnalyze(Cheat c) {
		if (!c.isActive())
			return;
		if (c.getKey().equalsIgnoreCase(CheatKeys.FORCEFIELD)) {
			if (timeStartFakePlayer == 0)
				timeStartFakePlayer = 1; // not on the player connection
			else
				makeAppearEntities();
		}
	}

	public void startAllAnalyze() {
		for (Cheat c : Cheat.values())
			startAnalyze(c);
	}

	@Override
	public void stopAnalyze(Cheat c) {

	}

	private void destroy() {
		saveData();
	}

	public void makeAppearEntities() {
		if (!Cheat.forKey(CheatKeys.FORCEFIELD).isActive() || Adapter.getAdapter().getConfig().getBoolean("cheats.forcefield.ghost_disabled"))
			return;
		timeStartFakePlayer = System.currentTimeMillis();
		spawnRight();
		spawnLeft();
		spawnBehind();
	}

	public void removeFakePlayer(FakePlayer fp, boolean detected) {
		if (!FAKE_PLAYER.contains(fp))
			return;
		FAKE_PLAYER.remove(fp);
		if (!detected) {
			if (fakePlayerTouched > 0)
				ForceFieldProtocol.manageForcefieldForFakeplayer(getPlayer(), this);
			if (FAKE_PLAYER.size() == 0)
				fakePlayerTouched = 0;
			return;
		}
		fakePlayerTouched++;
		long l = (System.currentTimeMillis() - timeStartFakePlayer);
		if (l >= 3000) {
			if (FAKE_PLAYER.size() == 0) {
				ForceFieldProtocol.manageForcefieldForFakeplayer(getPlayer(), this);
				fakePlayerTouched = 0;
			}
		} else {
			ForceFieldProtocol.manageForcefieldForFakeplayer(getPlayer(), this);
			if (fakePlayerTouched < 100) {
				spawnRandom();
				spawnRandom();
			}
		}
	}

	public void spawnRandom() {
		int choice = new Random().nextInt(3);
		if (choice == 0)
			spawnRight();
		else if (choice == 1)
			spawnBehind();
		else
			spawnLeft();
	}

	private void spawnRight() {
		Location<World> loc = getPlayer().getLocation().copy();
		Vector3d dir = getPlayer().getHeadRotation();
		double x = dir.getX(), z = dir.getZ();
		if (x >= 0 && z >= 0) {
			loc = loc.add(-1, 1, 1);
		} else if (x >= 0 && z <= 0) {
			loc = loc.add(-1, 1, 0);
		} else if (x <= 0 && z >= 0) {
			loc = loc.add(-1, 1, 0);
		} else if (x <= 0 && z <= 0) {
			loc = loc.add(-1, 1, 1);
		}
		FakePlayer fp = new FakePlayer(loc, getRandomFakePlayerName()).show(getPlayer());
		FAKE_PLAYER.add(fp);
	}

	private void spawnLeft() {
		Location<World> loc = getPlayer().getLocation().copy();
		Vector3d dir = getPlayer().getHeadRotation();
		double x = dir.getX(), z = dir.getZ();
		if (x >= 0 && z >= 0) {
			loc = loc.add(0, 1, -1);
		} else if (x >= 0 && z <= 0) {
			loc = loc.add(-1, 1, 1);
		} else if (x <= 0 && z >= 0) {
			loc = loc.add(1, 1, -1);
		} else if (x <= 0 && z <= 0) {
			loc = loc.add(1, 1, 1);
		}
		FakePlayer fp = new FakePlayer(loc, getRandomFakePlayerName()).show(getPlayer());
		FAKE_PLAYER.add(fp);
	}

	private void spawnBehind() {
		Location<World> loc = getPlayer().getLocation().copy();
		Vector3d dir = getPlayer().getHeadRotation();
		double x = dir.getX(), z = dir.getZ();
		if (x >= 0 && z >= 0) {
			loc = loc.add(1, 1, -1);
		} else if (x >= 0 && z <= 0) {
			loc = loc.add(1, 1, 1);
		} else if (x <= 0 && z >= 0) {
			loc = loc.add(1, 1, 1);
		} else if (x <= 0 && z <= 0) {
			loc = loc.add(1, 1, -1);
		}
		FakePlayer fp = new FakePlayer(loc, getRandomFakePlayerName()).show(getPlayer());
		FAKE_PLAYER.add(fp);
	}

	private String getRandomFakePlayerName() {
		Collection<Player> online = Sponge.getServer().getOnlinePlayers();
		if (online.size() <= 1) {
			return new Random().nextBoolean() ? "Elikill58" : "RedNesto";
		} else
			return online.stream().skip(new Random().nextInt(online.size())).findFirst().get().getName();
	}

	@Override
	public String getReason(Cheat c) {
		String n = "";
		for (Cheat all : Cheat.values())
			if (getAllWarn(all) > 5)
				n = n + (n.equals("") ? "" : ", ") + all.getName();
		if (!n.contains(c.getName()))
			n = n + (n.equals("") ? "" : ", ") + c.getName();
		return n;
	}

	public float getFallDistance() {
		return p.getOrCreate(FallDistanceData.class).get().fallDistance().get();
	}

	public List<PotionEffect> getActiveEffects() {
		return p.getOrCreate(PotionEffectData.class).get().asList();
	}

	public ItemType getItemTypeInHand() {
		Optional<ItemStack> item = p.getItemInHand(HandTypes.MAIN_HAND);
		return item.isPresent() ? item.get().getType() : ItemTypes.AIR;
	}

	public boolean hasPotionEffect(PotionEffectType type) {
		return hasPotionEffect(type.getId());
	}

	public boolean hasPotionEffect(String typeName) {
		for (PotionEffect pe : getActiveEffects())
			if (pe.getType().getId().equals(typeName))
				return true;
		return false;
	}

	public Vector3d getVelocity() {
		return p.getOrCreate(VelocityData.class).get().velocity().get();
	}

	public boolean isFlying() {
		return p.getOrCreate(FlyingData.class).get().flying().get();
	}

	public boolean isBlock(ItemType m) {
		return m.getBlock().isPresent();
	}

	@Override
	public boolean hasDefaultPermission(String s) {
		return p.hasPermission(s);
	}

	@Override
	public double getLife() {
		return p.get(HealthData.class).get().health().get();
	}

	@Override
	public String getName() {
		return p.getName();
	}

	@Override
	public String getGameMode() {
		return p.gameMode().get().getName();
	}

	@Override
	public float getWalkSpeed() {
		return (float) (double) p.get(Keys.WALKING_SPEED).get();
	}

	@Override
	public int getLevel() {
		return p.get(Keys.EXPERIENCE_LEVEL).get();
	}

	@Override
	public void kickPlayer(String reason, String time, String by, boolean def) {
		p.kick(Messages.getMessage(p, "ban.kick_" + (def ? "def" : "time"), "%reason%", reason, "%time%", String.valueOf(time), "%by%", by));
	}

	@Override
	public void banEffect() {
		System.out.println("[SpongeNegativityPlayer] SOOOON");
	}

	public List<PlayerCheatEvent.Alert> getAlertForAllCheat() {
		final List<PlayerCheatEvent.Alert> list = new ArrayList<>();
		pendingAlerts.forEach((c, listAlerts) -> {
			if (!listAlerts.isEmpty())
				list.add(getAlertForCheat(c, listAlerts));
		});
		return list;
	}

	public PlayerCheatEvent.Alert getAlertForCheat(Cheat c, List<PlayerCheatEvent.Alert> list) {
		int nb = 0, nbConsole = 0;
		HashMap<Integer, Integer> relia = new HashMap<>();
		HashMap<Integer, Integer> ping = new HashMap<>();
		ReportType type = ReportType.NONE;
		boolean hasRelia = false;
		CheatHover hoverProof = null;
		for (PlayerCheatEvent.Alert e : list) {
			nb += e.getNbAlert();

			relia.put(e.getReliability(), relia.getOrDefault(e.getReliability(), 0) + 1);

			ping.put(e.getPing(), ping.getOrDefault(e.getPing(), 0) + 1);

			if (type == ReportType.NONE || (type == ReportType.WARNING && e.getReportType() == ReportType.VIOLATION))
				type = e.getReportType();

			hasRelia = e.hasManyReliability() ? true : hasRelia;

			if (hoverProof == null && e.getHover() != null)
				hoverProof = e.getHover();

			nbConsole += e.getNbAlertConsole();
			e.clearNbAlertConsole();
		}
		// Don't to 100% each times that there is more than 2 alerts, we made a summary,
		// and a the nb of alert to upgrade it
		int newRelia = UniversalUtils.parseInPorcent(UniversalUtils.sum(relia) + nb);
		int newPing = UniversalUtils.sum(ping);
		// we can ignore "proof" and "stats_send" because they have been already saved
		// and they are NOT showed to player
		return new PlayerCheatEvent.Alert(type, getPlayer(), c, newRelia, hasRelia, newPing, "", hoverProof, nb, nbConsole);
	}

	public void fight() {
		isInFight = true;
		if (fightTask != null)
			fightTask.cancel();
		fightTask = Task.builder().delayTicks(40).execute(new Runnable() {
			@Override
			public void run() {
				isInFight = false;
			}
		}).submit(SpongeNegativity.INSTANCE);
	}

	public void unfight() {
		isInFight = false;
		if (fightTask != null)
			fightTask.cancel();
		fightTask = null;
	}

	public boolean hasThorns(Player p) {
		if (hasThornsForItem(p.getHelmet().orElse(null)))
			return true;
		if (hasThornsForItem(p.getChestplate().orElse(null)))
			return true;
		if (hasThornsForItem(p.getLeggings().orElse(null)))
			return true;
		if (hasThornsForItem(p.getBoots().orElse(null)))
			return true;
		return false;
	}

	private boolean hasThornsForItem(ItemStack item) {
		if (item == null)
			return false;
		Optional<EnchantmentData> opt = item.get(EnchantmentData.class);
		if (opt.isPresent())
			if (opt.get().contains(Enchantment.of(EnchantmentTypes.THORNS, 1)))
				return true;
		return false;
	}

	@Override
	public boolean isOp() {
		return false;
	}

	public static SpongeNegativityPlayer getNegativityPlayer(Player player) {
		return PLAYERS_CACHE.computeIfAbsent(player.getUniqueId(), id -> new SpongeNegativityPlayer(player));
	}

	public static SpongeNegativityPlayer getNegativityPlayer(User player) {
		return PLAYERS_CACHE.computeIfAbsent(player.getUniqueId(), id -> new SpongeNegativityPlayer(player));
	}

	public static Map<UUID, SpongeNegativityPlayer> getAllPlayers() {
		return PLAYERS_CACHE;
	}

	public static void removeFromCache(Player player) {
		removeFromCache(player.getUniqueId());
	}

	public static void removeFromCache(UUID playerId) {
		SpongeNegativityPlayer nPlayer = PLAYERS_CACHE.remove(playerId);
		if (nPlayer != null) {
			nPlayer.destroy();
		}
	}
}
