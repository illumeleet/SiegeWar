package com.gmail.goosius.siegewar.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.objects.Siege;
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.TextComponent;
import com.palmergames.bukkit.towny.event.asciimap.WildernessMapEvent;

public class TownyMapListener implements Listener {

	@EventHandler
	public void onWildernessMap(WildernessMapEvent event) {
		if (!SiegeController.hasActiveSiege(event.getWorldCoord()))
			return;
		
		Siege siege = SiegeController.getSiege(event.getWorldCoord());
		if (siege == null)
			return;
		
		event.setMapSymbol("S");
		TextComponent hoverText = Component.text("Siege of " + siege.getDefenderNameForDisplay() + " by " + siege.getAttackerNameForDisplay());
		event.setHoverText(hoverText);
		event.setClickCommand("/siegewar hud " + siege.getDefenderName());
	}
}
