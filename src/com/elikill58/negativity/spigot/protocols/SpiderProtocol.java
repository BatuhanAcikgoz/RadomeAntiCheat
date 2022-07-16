package com.elikill58.negativity.spigot.protocols;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import com.elikill58.negativity.spigot.SpigotNegativity;
import com.elikill58.negativity.spigot.SpigotNegativityPlayer;
import com.elikill58.negativity.spigot.blocks.SpigotLocation;
import com.elikill58.negativity.spigot.listeners.NegativityPlayerMoveEvent;
import com.elikill58.negativity.spigot.utils.LocationUtils;
import com.elikill58.negativity.spigot.utils.Utils;
import com.elikill58.negativity.universal.Cheat;
import com.elikill58.negativity.universal.CheatKeys;
import com.elikill58.negativity.universal.ReportType;
import com.elikill58.negativity.universal.Version;
import com.elikill58.negativity.universal.utils.UniversalUtils;

public class SpiderProtocol extends Cheat implements Listener {

	public SpiderProtocol() {
		super(CheatKeys.SPIDER, false, Material.SPIDER_EYE, CheatCategory.MOVEMENT, true, "wallhack",
				"wall");
	}

	@EventHandler
	public void onPlayerMove(NegativityPlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (!p.getGameMode().equals(GameMode.SURVIVAL) && !p.getGameMode().equals(GameMode.ADVENTURE))
			return;
		SpigotNegativityPlayer np = e.getNegativityPlayer();
		SpigotLocation loc = new SpigotLocation(p.getLocation());
		if (!np.hasDetectionActive(this) || LocationUtils.isUsingElevator(p))
			return;
		if (p.getFallDistance() != 0 || np.hasElytra() || p.isFlying() || p.hasPotionEffect(PotionEffectType.JUMP)
				|| !LocationUtils.hasOtherThan(loc, Material.AIR) || (e.getFrom().getX() == e.getTo().getX() && e.getFrom().getZ() == e.getTo().getZ()))
				return;
		Material underPlayer = loc.clone().subtract(0, 1, 0).getBlock().getType(),
				underUnder = loc.clone().subtract(0, 2, 0).getBlock().getType();
		if (!underPlayer.equals(Material.AIR) || !underUnder.equals(Material.AIR)
				|| !loc.getBlock().getType().equals(Material.AIR) || LocationUtils.isUsingElevator(p) || np.isUsingTrident())
			return;
		if(Version.getVersion().isNewerOrEquals(Version.V1_9) && p.hasPotionEffect(PotionEffectType.LEVITATION))
			return;
		if(hasBypassBlockAround(loc))
			return;
		double y = e.getTo().getY() - e.getFrom().getY();
		boolean isAris = ((float) y) == p.getWalkSpeed();
		if (((y > 0.499 && y < 0.7) || isAris) && !np.isUsingSlimeBlock && !p.isSprinting() && p.getVelocity().length() < 1.5) {
			int relia = UniversalUtils.parseInPorcent(y * 160 + (isAris ? 39 : 0));
			if (SpigotNegativity.alertMod((np.getWarn(this) > 6 ? ReportType.WARNING : ReportType.VIOLATION), p, this,
					relia, "Nothing around him. To > From: " + y + " isAris: " + isAris + ", has not stab slairs")
					&& isSetBack()) {
				Utils.teleportPlayerOnGround(p);
			}
		}
	}
	
	@EventHandler
	public void onPlayerContinueMove(NegativityPlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (!p.getGameMode().equals(GameMode.SURVIVAL) && !p.getGameMode().equals(GameMode.ADVENTURE))
			return;
		SpigotNegativityPlayer np = e.getNegativityPlayer();
		if (!np.hasDetectionActive(this) || p.isFlying() || p.isInsideVehicle() || p.getVehicle() != null || np.hasElytra() || np.isUsingSlimeBlock)
			return;
		SpigotLocation loc = new SpigotLocation(p.getLocation());
		if(hasBypassBlockAround(loc) || np.isUsingTrident())
			return;
		if(LocationUtils.hasExtended(loc, "STAIRS") || LocationUtils.isUsingElevator(p) || p.getLocation().getBlock().getType().name().contains("LAVA"))
			return;
		String blockName = p.getLocation().getBlock().getType().name();
		if(blockName.contains("LADDER") || blockName.contains("VINE") || blockName.contains("SNOW"))
			return;
		if(Version.getVersion().isNewerOrEquals(Version.V1_9) && p.hasPotionEffect(PotionEffectType.LEVITATION))
			return;
		
		double y = e.getTo().getY() - e.getFrom().getY();
		if (np.lastSpiderLoc != null && np.lastSpiderLoc.getWorld() != null && Bukkit.getWorld(np.lastSpiderLoc.getWorld().getName()) != null && np.lastSpiderLoc.getWorld().equals(loc.getWorld()) && y > 0) {
			double tempDis = loc.getY() - np.lastSpiderLoc.getY(), lastSpiderDistance = np.contentDouble.getOrDefault("spider-last-distance", 0.0);
			if (lastSpiderDistance == tempDis && tempDis != 0) {
				np.SPIDER_SAME_DIST++;
				if(np.SPIDER_SAME_DIST > 2) {
					if (SpigotNegativity.alertMod(ReportType.WARNING, p, this, UniversalUtils.parseInPorcent(tempDis * 400 + np.SPIDER_SAME_DIST),
							"Nothing strange around him. To > From: " + y + ", distance: " + lastSpiderDistance + ". Walk with same y " + np.SPIDER_SAME_DIST + " times") && isSetBack()) {
						Utils.teleportPlayerOnGround(p);
					}
				}
			} else
				np.SPIDER_SAME_DIST = 0;
			np.contentDouble.put("spider-last-distance", tempDis);
		}
		np.lastSpiderLoc = loc;
	}
		
	private boolean hasBypassBlockAround(SpigotLocation loc) {
		if(has(loc, "SLAB", "STAIRS", "VINE", "LADDER", "WATER", "SCAFFOLD", "CAKE"))
			return true;
		loc = loc.clone().subtract(0, 1, 0);
		if(has(loc, "SLAB", "STAIRS", "VINE", "LADDER", "WATER", "SCAFFOLD", "CAKE"))
			return true;
		return false;
	}

	public boolean has(SpigotLocation loc, String... m) {
		String b = loc.getBlock().getType().name(),
				b1 = loc.clone().add(0, 0, 1).getBlock().getType().name(),
				b2 = loc.clone().add(1, 0, -1).getBlock().getType().name(),
				b3 = loc.clone().add(-1, 0, -1).getBlock().getType().name(),
				b4 = loc.clone().add(-1, 0, 1).getBlock().getType().name();
		for(String temp : m) {
			if(b.contains(temp))
				return true;
			if(b1.contains(temp))
				return true;
			if(b2.contains(temp))
				return true;
			if(b3.contains(temp))
				return true;
			if(b4.contains(temp))
				return true;
		}
		return false;
	}
}
