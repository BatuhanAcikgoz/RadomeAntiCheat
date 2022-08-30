package com.elikill58.negativity.spigot.protocols;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.elikill58.negativity.spigot.SpigotNegativity;
import com.elikill58.negativity.spigot.SpigotNegativityPlayer;
import com.elikill58.negativity.spigot.blocks.SpigotLocation;
import com.elikill58.negativity.spigot.listeners.NegativityPlayerMoveEvent;
import com.elikill58.negativity.spigot.packets.AbstractPacket;
import com.elikill58.negativity.spigot.packets.event.PacketReceiveEvent;
import com.elikill58.negativity.spigot.support.EssentialsSupport;
import com.elikill58.negativity.spigot.utils.ItemUtils;
import com.elikill58.negativity.spigot.utils.LocationUtils;
import com.elikill58.negativity.universal.Cheat;
import com.elikill58.negativity.universal.CheatKeys;
import com.elikill58.negativity.universal.PacketType;
import com.elikill58.negativity.universal.ReportType;
import com.elikill58.negativity.universal.Version;
import com.elikill58.negativity.universal.adapter.Adapter;
import com.elikill58.negativity.universal.utils.UniversalUtils;

public class NoFallProtocol extends Cheat implements Listener {

	public NoFallProtocol() {
		super(CheatKeys.NO_FALL, false, ItemUtils.RED_WOOL, CheatCategory.MOVEMENT, true, "fall");
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerMove(NegativityPlayerMoveEvent e) {
		Player p = e.getPlayer();
		SpigotNegativityPlayer np = e.getNegativityPlayer();
		if (!np.hasDetectionActive(this) || e.isCancelled())
			return;
		if (!p.getGameMode().equals(GameMode.SURVIVAL) && !p.getGameMode().equals(GameMode.ADVENTURE))
			return;
		if (np.getAllowFlight() || np.hasElytra() || p.getVehicle() != null || p.hasPotionEffect(PotionEffectType.SPEED))
			return;
		if(Version.getVersion().isNewerOrEquals(Version.V1_13) && p.hasPotionEffect(PotionEffectType.SLOW_FALLING))
			return;
		SpigotLocation from = e.getFrom(), to = e.getTo();
		if(LocationUtils.hasMaterialsAround(to, "WATER", "BUBBLE"))
			return;
		double distance = to.toVector().distance(from.toVector());
		Block b = p.getLocation().getBlock();
		SpigotLocation locDown = new SpigotLocation(b.getRelative(BlockFace.DOWN).getLocation());
		SpigotLocation locUp = new SpigotLocation(b.getRelative(BlockFace.UP).getLocation());
		double motionY = from.getY() - to.getY();
		if (np.isOnGround()
				&& locDown.getBlock().getType().equals(Material.AIR) && !np.isInFight && !isWaterLogged(locDown.getBlock())
				&& ((motionY > p.getWalkSpeed() && p.getFallDistance() == 0) || (motionY > (p.getWalkSpeed() / 2)))
				&& p.getFallDistance() > 0.2 && p.getWalkSpeed() > p.getFallDistance()
				&& !LocationUtils.hasMaterialsAround(locDown, "STAIRS", "SCAFFOLD", "SLAB", "HONEY_BLOCK", "WATER", "BUBBLE")
				&& to.clone().add(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ()).getBlock().getType().equals(Material.AIR)) {
			if (locUp.getBlock().getType().name().contains("WATER"))
				np.useAntiNoFallSystem = true;
			if (!np.useAntiNoFallSystem) {
				int porcent = UniversalUtils.parseInPorcent(900 * motionY);
				SpigotNegativity.alertMod(ReportType.WARNING, p, this, porcent,
						"New NoFall - Player on ground. motionY: " + motionY + ", walkSpeed: " + p.getWalkSpeed()
								+ ", onGround: " + np.isOnGround() + ", fallDistance: " + p.getFallDistance(),
						new Cheat.CheatHover.Literal("MotionY (on ground): " + motionY));
			}
		} else if(motionY < 0.1)
			np.useAntiNoFallSystem = false;

		if (!(distance == 0.0D || from.getY() < to.getY())) {
			if (p.getFallDistance() == 0.0F && locDown.getBlock().getType().equals(Material.AIR)) {
				int relia = UniversalUtils.parseInPorcent(distance * 100);
				if (np.isOnGround() && !np.isUsingSlimeBlock) {
					Vector direction = p.getVelocity().clone();
					double distanceVector = to.toVector().clone().add(direction).distance(from.toVector());
					double disWithDirY = from.clone().add(direction).toVector().setY(0).distanceSquared(to.toVector().setY(0));
					if ((distance - Math.abs(distanceVector)) > 0.79D && !(p.getWalkSpeed() > 0.45F && SpigotNegativity.essentialsSupport
							&& EssentialsSupport.checkEssentialsSpeedPrecondition(p)) && !np.isInFight) {
						boolean mayCancel = SpigotNegativity.alertMod(ReportType.VIOLATION, p, this, relia,
								"Player in ground. FallDamage: " + p.getFallDistance() + ", DistanceFrom/To: "
										+ distance + ", Velocity Y: " + p.getVelocity().getY() + ", distanceVector: " + distanceVector + ", disDirY: " + disWithDirY);
						if (mayCancel)
							np.NO_FALL_DAMAGE += 1;
					} else if (np.NO_FALL_DAMAGE != 0) {
						if (isSetBack())
							manageDamage(p, np.NO_FALL_DAMAGE, relia);
						np.NO_FALL_DAMAGE = 0;
					}
				} else {
					if (distance > 2D) {
						boolean mayCancel = SpigotNegativity.alertMod(ReportType.VIOLATION, p, this, relia,
								"Player not in ground no fall Damage. FallDistance: " + p.getFallDistance()
										+ ", DistanceBetweenFromAndTo: " + distance + " . Warn: " + np.getWarn(this));
						if (mayCancel)
							np.NO_FALL_DAMAGE += 1;
					} else if (np.NO_FALL_DAMAGE != 0) {
						if (isSetBack())
							manageDamage(p, np.NO_FALL_DAMAGE, relia);
						np.NO_FALL_DAMAGE = 0;
					}
				}
			} else if (!np.isOnGround()) {
				Material justUnder = p.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();
				if (justUnder.isSolid() && p.getFallDistance() > 3.0 && !np.isInFight) {
					int relia = UniversalUtils.parseInPorcent(100 - (np.ping / 5) + p.getFallDistance());
					boolean mayCancel = SpigotNegativity.alertMod(ReportType.VIOLATION, p, this, relia,
							"Player not ground with fall damage (FallDistance: " + p.getFallDistance()
									+ "). Block 0.1 below: " + justUnder.name() + ", DistanceBetweenFromAndTo: "
									+ distance + " .Warn: " + np.getWarn(this));
					if (mayCancel && isSetBack())
						manageDamage(p, (int) p.getFallDistance(), relia);
				}
			}
		}
	}

	private boolean isWaterLogged(Block b) {
		return Version.getVersion().isNewerOrEquals(Version.V1_13) && (b instanceof Waterlogged) && ((Waterlogged) b).isWaterlogged();
	}

	private void manageDamage(Player p, int damage, int relia) {
		Adapter ada = Adapter.getAdapter();
		p.damage(
				damage >= p.getHealth()
						? (ada.getConfig().getBoolean("cheats.nofall.kill")
								&& ada.getConfig().getDouble("cheats.nofall.kill-reliability") >= relia ? damage
										: p.getHealth() - 0.5)
						: damage);
	}

	@EventHandler
	public void onNegPacket(PacketReceiveEvent e) {
		AbstractPacket pa = e.getPacket();
		PacketType type = pa.getPacketType();
		if (!(type.equals(PacketType.Client.FLYING) || type.equals(PacketType.Client.POSITION) || type.equals(PacketType.Client.POSITION_LOOK)))
			return;
		Bukkit.getScheduler().runTaskAsynchronously(SpigotNegativity.getInstance(), () -> {
			Player p = e.getPlayer();
			SpigotNegativityPlayer np = SpigotNegativityPlayer.getNegativityPlayer(p);
			if (!np.hasDetectionActive(this))
				return;
			if (type.equals(PacketType.Client.FLYING)) {
				if (pa.getContent().getBooleans().read(0) && np.contentBoolean.getOrDefault("packet-going-down", false)
						&& np.lastPacketType != type && p.getFallDistance() > 0.3) {
					SpigotNegativity.alertMod(ReportType.WARNING, p, this, 99, "Player going down, last PackeType: "
							+ np.lastPacketType.getFullName() + ", fallDistance: " + p.getFallDistance());
				}
			} else if (type.equals(PacketType.Client.POSITION) || type.equals(PacketType.Client.POSITION_LOOK)) {
				double newY = getY(pa.getPacket());
				np.contentBoolean.put("packet-going-down", np.contentDouble.getOrDefault("packet-diff-y", 0.0) > newY);
				np.contentDouble.put("packet-diff-y", newY);
			}
			np.lastPacketType = type;
		});
	}

	public double getY(Object obj) {
		try {
			Field f = obj.getClass().getSuperclass().getDeclaredField(Version.getVersion().isNewerOrEquals(Version.V1_17) ? "b" : "y");
			f.setAccessible(true);
			return f.getDouble(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
