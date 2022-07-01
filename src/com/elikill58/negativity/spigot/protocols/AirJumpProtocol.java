package com.elikill58.negativity.spigot.protocols;

import static com.elikill58.negativity.spigot.utils.LocationUtils.hasOtherThanExtended;
import static com.elikill58.negativity.spigot.utils.LocationUtils.hasOtherThan;
import static com.elikill58.negativity.universal.utils.UniversalUtils.parseInPorcent;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.elikill58.negativity.spigot.SpigotNegativity;
import com.elikill58.negativity.spigot.SpigotNegativityPlayer;
import com.elikill58.negativity.spigot.listeners.NegativityPlayerMoveEvent;
import com.elikill58.negativity.spigot.utils.Utils;
import com.elikill58.negativity.universal.Cheat;
import com.elikill58.negativity.universal.CheatKeys;
import com.elikill58.negativity.universal.ReportType;

public class AirJumpProtocol extends Cheat implements Listener {

	public AirJumpProtocol() {
		super(CheatKeys.AIR_JUMP, false, Material.FEATHER, CheatCategory.MOVEMENT, true, "airjump", "air", "jump");
	}

	@EventHandler
	public void onMove(NegativityPlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (!p.getGameMode().equals(GameMode.SURVIVAL) && !p.getGameMode().equals(GameMode.ADVENTURE))
			return;
		SpigotNegativityPlayer np = e.getNegativityPlayer();
		if (!np.hasDetectionActive(this) || np.hasPotionEffect("JUMP_BOOST"))
			return;
		Location loc = p.getLocation().clone();
		if (p.isFlying() || p.getVehicle() != null || np.isUsingTrident() || np.hasElytra() || np.isInFight || loc.getBlock().getType().name().contains("STAIR"))
			return;
		Location locDown = loc.clone().subtract(0, 1, 0), locDownDown = locDown.clone().subtract(0, 1, 0);
		Bukkit.getScheduler().runTaskLater(SpigotNegativity.getInstance(), () -> {
			if(hasOtherThanExtended(loc, "AIR") || hasOtherThan(locDown, "AIR") || hasOtherThan(locDownDown, "AIR"))
				return;
			if(locDownDown.getBlock().getType().name().contains("STAIR") || locDown.getBlock().getType().name().contains("STAIR"))
				return;
			
			boolean mayCancel = false;
			String allTypes = ", actual: " + loc.getBlock().getType().name() + ", down: " + locDown.getBlock().getType().name() + ", Down Down: " + locDownDown.getBlock().getType().name();
			double diffYtoFromBasic = e.getTo().getY() - e.getFrom().getY();
			double diffYtoFrom = diffYtoFromBasic - Math.abs(e.getTo().getDirection().getY());
			if (diffYtoFrom > 0.35 && np.lastYDiff < diffYtoFrom && np.lastYDiff > p.getVelocity().getY() && !hasOtherThanExtended(locDownDown, "AIR")) {
				mayCancel = SpigotNegativity.alertMod(
						diffYtoFrom > 0.5 && np.getWarn(this) > 5 ? ReportType.VIOLATION : ReportType.WARNING, p, this,
								parseInPorcent((int) (diffYtoFrom * 210) - np.ping), "Actual diff Y: " + diffYtoFrom + ", last diff Y: " + np.lastYDiff + allTypes + ", velY: " + p.getVelocity().getY());
			}
			np.lastYDiff = diffYtoFrom;
			
			boolean wasGoingDown = np.contentBoolean.getOrDefault("going-down", false);
			double d = np.contentDouble.getOrDefault("airjump-diff-y", 0.0);
			if(diffYtoFromBasic > d && wasGoingDown && diffYtoFromBasic != 0.5 && locDown.getBlock().getType().name().equalsIgnoreCase("AIR") && p.getVelocity().getY() < 0.5) { // 0.5 when use stairs or slab
				mayCancel = SpigotNegativity.alertMod(ReportType.WARNING, p, this, parseInPorcent(diffYtoFrom * 200),
						"Was going down, lastY " + d + ", current: " + diffYtoFrom + allTypes + ", velY: " + p.getVelocity().getY() + ", diffY base: " + diffYtoFromBasic) || mayCancel;
			}
			np.contentDouble.put("airjump-diff-y", diffYtoFrom);
			np.contentBoolean.put("going-down", diffYtoFrom < 0);
			if (isSetBack() && mayCancel)
				Utils.teleportPlayerOnGround(p);
		}, 5);
	}
}
