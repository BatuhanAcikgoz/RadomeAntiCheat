package com.elikill58.negativity.spigot.protocols;

import static com.elikill58.negativity.spigot.utils.LocationUtils.hasMaterialsAround;
import static com.elikill58.negativity.spigot.utils.LocationUtils.hasOtherThan;

import java.text.NumberFormat;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.elikill58.negativity.spigot.SpigotNegativity;
import com.elikill58.negativity.spigot.SpigotNegativityPlayer;
import com.elikill58.negativity.spigot.blocks.SpigotLocation;
import com.elikill58.negativity.spigot.listeners.NegativityPlayerMoveEvent;
import com.elikill58.negativity.spigot.support.EssentialsSupport;
import com.elikill58.negativity.spigot.utils.LocationUtils;
import com.elikill58.negativity.spigot.utils.Utils;
import com.elikill58.negativity.universal.Cheat;
import com.elikill58.negativity.universal.CheatKeys;
import com.elikill58.negativity.universal.ReportType;
import com.elikill58.negativity.universal.adapter.Adapter;
import com.elikill58.negativity.universal.utils.UniversalUtils;

public class SpeedProtocol extends Cheat implements Listener {

	private NumberFormat numberFormat = NumberFormat.getInstance();

	public SpeedProtocol() {
		super(CheatKeys.SPEED, false, Material.BEACON, CheatCategory.MOVEMENT, true, "speed", "speedhack");
		numberFormat.setMaximumFractionDigits(4);
	}

	@EventHandler
	public void onPlayerMove(NegativityPlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (!p.getGameMode().equals(GameMode.SURVIVAL) && !p.getGameMode().equals(GameMode.ADVENTURE))
			return;
		SpigotNegativityPlayer np = e.getNegativityPlayer();
		if (!np.hasDetectionActive(this))
			return;
		if (np.hasElytra() || LocationUtils.isUsingElevator(p) || np.TIME_INVINCIBILITY_SPEED > System.currentTimeMillis())
			return;
		int amplifier = 0;
		if (p.hasPotionEffect(PotionEffectType.SPEED)) {
			amplifier = np.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
			if (amplifier > 5) {
				np.TIME_INVINCIBILITY_SPEED = System.currentTimeMillis() + amplifier * 100;
				return;
			}
		}
		SpigotLocation from = e.getFrom().clone(), to = e.getTo().clone();
		if (p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.SPONGE) || p.getEntityId() == 100 || p.getVehicle() != null || np.getAllowFlight()
				|| p.getFlySpeed() > 3.0F || p.getWalkSpeed() > 2.0F || np.hasPotionEffect("DOLPHINS_GRACE") || p.isInsideVehicle() || hasEnderDragonAround(p) || np.isUsingTrident())
			return;
		if (np.BYPASS_SPEED != 0) {
			np.BYPASS_SPEED--;
			return;
		}

		SpigotLocation loc = new SpigotLocation(p.getLocation()), locDown = loc.clone().subtract(0, 1, 0), locUp = loc.clone().add(0, 1, 0);
		double dif = to.getY() - from.getY();
		boolean hasIce = locDown.getBlock().getType().name().contains("ICE") || locUp.getBlock().getType().name().contains("ICE"), onGround = np.isOnGround();
		if (hasIce) {
			np.contentBoolean.put("speed-has-ice", true);
			Adapter.getAdapter().debug("Has ice below " + p.getName());
		} else
			hasIce = np.contentBoolean.getOrDefault("speed-has-ice", false);

		if (onGround && dif < 0) {
			int firstIce = np.contentInts.getOrDefault("speed-has-ice-first", 5);
			if (firstIce <= 0) {
				Adapter.getAdapter().debug("Removing ice bypass for " + p.getName());
				np.contentBoolean.remove("speed-has-ice");
				np.contentInts.remove("speed-has-ice-first");
			} else {
				np.contentInts.put("speed-has-ice-first", firstIce - 1);
			}
		}

		if (hasIce || hasMaterialsAround(loc, "ICE", "TRAPDOOR", "SLAB", "STAIR", "CARPET") || hasMaterialsAround(locDown, "ICE", "TRAPDOOR", "SLAB", "STAIR", "CARPET")
				|| hasMaterialsAround(locUp, "ICE", "TRAPDOOR", "SLAB", "STAIR", "CARPET") || hasMaterialsAround(loc.clone().add(0, 2, 0), "ICE", "TRAPDOOR", "SLAB", "STAIR", "CARPET"))
			return;
		double y = to.toVector().clone().setY(0).distance(from.toVector().clone().setY(0)), velocity = p.getVelocity().getY(), velLen = p.getVelocity().length();
		boolean mayCancel = false;
		if (onGround) {
			Vector direction = p.getVelocity().clone();
			double disWithDir = from.clone().add(direction).distanceSquared(to);
			double disWithDirY = from.clone().add(direction).toVector().setY(0).distanceSquared(to.toVector().setY(0));
			double walkSpeed = (SpigotNegativity.essentialsSupport ? (p.getWalkSpeed() - EssentialsSupport.getEssentialsRealMoveSpeed(p)) : p.getWalkSpeed()) + amplifier * 0.1;
			boolean walkTest = y > walkSpeed * 3.1 && y > 0.65D, walkWithEssTest = (y - walkSpeed > (walkSpeed * 2.5));
			if (((SpigotNegativity.essentialsSupport ? (walkWithEssTest || (p.getWalkSpeed() < 0.35 && y >= 0.75D)) : y >= 0.75D) || walkTest) && (y < (disWithDir + disWithDirY))
					&& !(walkSpeed == 0.1599999964237213 && np.isBedrockPlayer())) {
				int porcent = UniversalUtils.parseInPorcent(y * 50 + (walkTest ? 10 : 0) + (walkWithEssTest == walkTest ? 10 : 0) + (walkWithEssTest ? 10 : 0));
				ReportType type = np.getWarn(this) > 7 ? ReportType.VIOLATION : ReportType.WARNING;
				String proof = "On ground. WalkSpeed: " + walkSpeed + ", Distance from/to: " + y + ", walkTest: " + walkTest + ", walkWithEss: " + walkWithEssTest + ", y: " + y
						+ ", disDir: " + disWithDir + ", disDirY: " + disWithDirY;
				mayCancel = SpigotNegativity.alertMod(type, p, this, porcent, proof, hoverMsg("distance_ground", "%distance%", numberFormat.format(y)));
			}
			double calculatedSpeedWith = Utils.getSpeed(from, to);
			double calculatedSpeedWithoutY = Utils.getSpeed(from, to, p.getVelocity());
			if (calculatedSpeedWithoutY > (p.getWalkSpeed() + 0.01) && velocity < calculatedSpeedWithoutY && !hasOtherThan(from.clone().add(0, 1, 0), "AIR") && Math.abs(velocity) > 0.1
					&& calculatedSpeedWithoutY > velLen && calculatedSpeedWithoutY != calculatedSpeedWith
					&& (p.getWalkSpeed() * 2 < calculatedSpeedWith || (p.getWalkSpeed() + 0.1 < calculatedSpeedWithoutY)) && dif != 0) { // "+0.01" if to prevent lag
				mayCancel = SpigotNegativity.alertMod(ReportType.WARNING, p, this, 90, "Calculated speed: " + calculatedSpeedWithoutY + ", Walk Speed: " + p.getWalkSpeed() + ", Velocity Y: "
						+ p.getVelocity().toString() + ", speed: " + calculatedSpeedWith);
			}
		} else {
			for (Entity entity : p.getNearbyEntities(5, 5, 5))
				if (entity instanceof Creeper || entity.getType().equals(EntityType.CREEPER))
					return;
			if (!mayCancel) {
				if (y >= 0.85D && (p.getWalkSpeed() * 1.1) < y) {
					mayCancel = SpigotNegativity.alertMod(np.getWarn(this) > 7 ? ReportType.VIOLATION : ReportType.WARNING, p, this, UniversalUtils.parseInPorcent(y * 100 * 2),
							"Player NOT in ground. WalkSpeed: " + p.getWalkSpeed() + " Distance between from/to location: " + y,
							hoverMsg("distance_jumping", "%distance%", numberFormat.format(y)));
				} else {
					if (p.hasPotionEffect(PotionEffectType.JUMP))
						return;
					Material under = e.getTo().clone().subtract(0, 1, 0).getBlock().getType();
					if (!under.name().contains("STEP") && !np.isUsingSlimeBlock && !(under.name().contains("WATER") || Utils.isSwimming(p))) {
						double distance = from.distance(to);
						to.setY(from.getY());
						double yy = to.distance(from);
						if (distance > 0.45 && (distance > (yy * 2)) && p.getFallDistance() < 1) {
							np.SPEED_NB++;
							if (np.SPEED_NB > 4)
								mayCancel = SpigotNegativity.alertMod(ReportType.WARNING, p, this, UniversalUtils.parseInPorcent(86 + np.SPEED_NB),
										"HighSpeed - Block under: " + under.name() + ", Speed: " + distance + ", nb: " + np.SPEED_NB + ", fallDistance: " + p.getFallDistance());
						} else
							np.SPEED_NB = 0;
					}
				}
			}
		}
		if (isSetBack() && mayCancel)
			e.setCancelled(true);
	}

	private boolean hasEnderDragonAround(Player p) {
		for (EnderDragon et : p.getWorld().getEntitiesByClass(EnderDragon.class))
			if (et.getLocation().distance(p.getLocation()) < 15)
				return true;
		return false;
	}

	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player)
			SpigotNegativityPlayer.getNegativityPlayer((Player) e.getEntity()).BYPASS_SPEED = 3;
	}

	@Override
	public boolean isBlockedInFight() {
		return true;
	}
}
