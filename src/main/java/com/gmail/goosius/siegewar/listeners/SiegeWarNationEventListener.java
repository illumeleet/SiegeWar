package com.gmail.goosius.siegewar.listeners;

import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.SiegeWar;
import com.gmail.goosius.siegewar.TownOccupationController;
import com.gmail.goosius.siegewar.enums.SiegeSide;
import com.gmail.goosius.siegewar.enums.SiegeWarPermissionNodes;
import com.gmail.goosius.siegewar.metadata.NationMetaDataController;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.gmail.goosius.siegewar.settings.Translation;
import com.gmail.goosius.siegewar.utils.PermissionUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarMoneyUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarNationUtil;
import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.event.NationBonusCalculationEvent;
import com.palmergames.bukkit.towny.event.NationPreRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.event.RenameNationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumOnlinePlayersCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumResidentsCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumTownsCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.townblockstatus.NationZoneTownBlockStatusEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author LlmDl
 *
 */
public class SiegeWarNationEventListener implements Listener {

	@SuppressWarnings("unused")
	private final SiegeWar plugin;
	
	public SiegeWarNationEventListener(SiegeWar instance) {

		plugin = instance;
	}

	@EventHandler
	public void onNationRankGivenToPlayer(NationRankAddEvent event) {
		//In Siegewar, if target town is peaceful, can't add military rank
		if(SiegeWarSettings.getWarSiegeEnabled()
			&& SiegeWarSettings.getWarCommonPeacefulTownsEnabled()
			&& PermissionUtil.doesNationRankAllowPermissionNode(event.getRank(), SiegeWarPermissionNodes.SIEGEWAR_NATION_SIEGE_BATTLE_POINTS)
			&& TownyAPI.getInstance().getResidentTownOrNull(event.getResident()).isNeutral()) { // We know that the resident's town will not be null based on the tests already done.
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("plugin_prefix") + Translation.of("msg_war_siege_cannot_add_nation_military_rank_to_peaceful_resident"));
		}
		
	}
	
	/*
	 * Simply saving the siege will set the name of the siege.
	 */
	@EventHandler
	public void onNationRename(RenameNationEvent event) {
		if (SiegeController.hasSieges(event.getNation())) {
			for (Siege siege : SiegeController.getSieges(event.getNation()))
				SiegeController.saveSiege(siege);
		}
	}
	
	/*
	 * SiegeWar will disable nation-zones if the town has a siege.
	 */
	@EventHandler
	public void onNationZoneStatus(NationZoneTownBlockStatusEvent event) {
		if (SiegeWarSettings.getWarSiegeEnabled() 
			&& SiegeController.hasActiveSiege(event.getTown()))	{
			event.setCancelled(true);
		}
	}
	
	/*
	 * SiegeWar will add lines to Nation which have a siege
	 */
    @EventHandler
	public void onNationStatusScreen(NationStatusScreenEvent event) {
		if (SiegeWarSettings.getWarSiegeEnabled()) {
			Nation nation = event.getNation();

			// Occupied Home Towns[3]: Town1, Town2, Town3
			List<Town> occupiedHomeTowns = TownOccupationController.getOccupiedHomeTowns(nation);
			String[] formattedOccupiedHomeTowns = TownyFormatter.getFormattedNames(occupiedHomeTowns.toArray(new Town[0]));
			List<String> out = new ArrayList<>(ChatTools.listArr(formattedOccupiedHomeTowns, Translation.of("status_nation_occupied_home_towns", occupiedHomeTowns.size())));

			// Occupied Foreign Towns[3]: Town4, Town5, Town6
			List<Town> occupiedForeignTowns = TownOccupationController.getOccupiedForeignTowns(nation);
			String[] formattedOccupiedForeignTowns = TownyFormatter.getFormattedNames(occupiedForeignTowns.toArray(new Town[0]));
			out.addAll(new ArrayList<>(ChatTools.listArr(formattedOccupiedForeignTowns, Translation.of("status_nation_occupied_foreign_towns", occupiedForeignTowns.size()))));

			// Siege Attacks [3]: TownA, TownB, TownC
	        List<Town> siegeAttacks = getTownsUnderSiegeAttack(nation);
	        String[] formattedSiegeAttacks = TownyFormatter.getFormattedNames(siegeAttacks.toArray(new Town[0]));
	        out.addAll(new ArrayList<>(ChatTools.listArr(formattedSiegeAttacks, Translation.of("status_nation_siege_attacks", siegeAttacks.size()))));

	        // Siege Defences [3]: TownX, TownY, TownZ
	        List<Town> siegeDefences = getTownsUnderSiegeDefence(nation);
	        String[] formattedSiegeDefences = TownyFormatter.getFormattedNames(siegeDefences.toArray(new Town[0]));
	        out.addAll(ChatTools.listArr(formattedSiegeDefences, Translation.of("status_nation_siege_defences", siegeDefences.size())));
	        
	        event.addLines(out);

			if (SiegeWarSettings.getWarSiegeNationStatisticsEnabled()) {
				event.addLines(Arrays.asList(Translation.of("status_nation_town_stats", NationMetaDataController.getTotalTownsGained(nation), NationMetaDataController.getTotalTownsLost(nation)),
											Translation.of("status_nation_plunder_stats", NationMetaDataController.getTotalPlunderGained(nation), NationMetaDataController.getTotalPlunderLost(nation))));
			}
		}
	}
    
	public static List<Town> getTownsUnderSiegeAttack(Nation nation) {
		List<Town> result = new ArrayList<>();
		for(Siege siege : SiegeController.getSieges(nation)) {
			if(siege.getStatus().isActive())
				result.add(siege.getTown());
		}
		return result;
	}

	public static List<Town> getTownsUnderSiegeDefence(Nation nation) {
		List<Town> result = new ArrayList<Town>();
		for(Town town: nation.getTowns()) {
			if(SiegeController.hasActiveSiege(town))
				result.add(town);
		}
		return result;
	}
	
	/*
	 * A nation being deleted with a siege means the siege ends,
	 * and a king may receive a refund.
	 */
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onDeleteNation(PreDeleteNationEvent event) {
		/*
		 * If SiegeWar and the Economy are enabled, give the player a nation refund if it is non-zero & enabled.
		 */
		if (SiegeWarSettings.getWarSiegeEnabled() 
				&& TownyEconomyHandler.isActive()
				&& SiegeWarSettings.getWarSiegeRefundInitialNationCostOnDelete() 
				&& SiegeWarSettings.getWarSiegeNationCostRefundPercentageOnDelete() > 0 
				&& event.getNation().getKing() != null) {
			SiegeWarMoneyUtil.makeNationRefundAvailable(event.getNation().getKing());
		}
		
		/*
		 * Remove any siege if the nation is deleted, regardless of whether SW is currently enabled.
		 */
		for (Siege siege : SiegeController.getSiegesByNationUUID(event.getNation().getUUID())) {
			SiegeController.removeSiege(siege, SiegeSide.DEFENDERS);
		}

		/*
		 * Remove any town occupation data associated with that nation
		 */
		TownOccupationController.removeForeignTownOccupations(event.getNation());
	}

	@EventHandler
	public void onPreNationEnemyRemove(NationPreRemoveEnemyEvent event) {
		Nation nation = event.getNation();
		Nation enemyNation = event.getEnemy();

		if (!SiegeController.hasSieges(nation))	
			return;
		
		List<Town> enemyTownsUnderSiege = SiegeController.getSiegedTowns(enemyNation);

		for (Siege siege : SiegeController.getSieges(nation)) {
			if (enemyTownsUnderSiege.contains(siege.getTown())) {
				event.setCancelled(true);
				event.setCancelMessage(Translation.of("plugin_prefix") + Translation.of("msg_err_cannot_remove_enemy"));
			}
		}
	}

	/**
	 * Updates the number of bonus blocks when Towny calculates it
	 *
	 * All unoccupied home towns are counted
	 * All occupied foreign towns are counted
	 */
	@EventHandler
	public void on(NationBonusCalculationEvent event) {
		Nation effectiveNation = SiegeWarNationUtil.getEffectiveNation(event.getNation());
		int bonusBlocks = (Integer) TownySettings.getNationLevel(effectiveNation).get(TownySettings.NationLevel.TOWN_BLOCK_LIMIT_BONUS);
		event.setBonusBlocks(bonusBlocks);
	}

	/**
	 * Update the nation numresidents calculation when towny displays the nations list
	 *
	 * All unoccupied home towns are counted
	 * All occupied foreign towns are counted
	 */
	@EventHandler
	public void on(NationListDisplayedNumResidentsCalculationEvent event) {
		Nation effectiveNation = SiegeWarNationUtil.getEffectiveNation(event.getNation());
		event.setDisplayedValue(effectiveNation.getNumResidents());
	}

	/**
	 * Update the nation numtowns calculation when towny displays the nations list
	 *
	 * All unoccupied home towns are counted
	 * All occupied foreign towns are counted
	 */
	@EventHandler
	public void on(NationListDisplayedNumTownsCalculationEvent event) {
		Nation effectiveNation = SiegeWarNationUtil.getEffectiveNation(event.getNation());
		event.setDisplayedValue(effectiveNation.getNumTowns());
	}

	/**
	 * Update the nation numtownblocks calculation when towny displays the nations list
	 *
	 * All unoccupied home towns are counted
	 * All occupied foreign towns are counted
	 */
	/*
	TODO - ENABLE ME WHEN THE REQUIRED TOWNY CODE GETS ON MAVEN
	@EventHandler
	public void on(NationListDisplayedNumTownBlocksCalculationEvent event) {
		Nation effectiveNation = SiegeWarNationUtil.getEffectiveNation(event.getNation());
		event.setDisplayedValue(effectiveNation.getNumTownblocks());
	}
	*/

	/**
	 * Update the nation onlineplayers calculation when towny displays the nations list
	 *
	 * All unoccupied home towns are counted
	 * All occupied foreign towns are counted
	 */
	@EventHandler
	public void on(NationListDisplayedNumOnlinePlayersCalculationEvent event) {
		int effectiveNumOnlinePlayers = 0;
		Resident resident;
		Nation effectiveNation = SiegeWarNationUtil.getEffectiveNation(event.getNation());
		for(Player player: BukkitTools.getOnlinePlayers()) {
			if(TownyUniverse.getInstance().hasResident(player.getUniqueId())) {
				resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
				try {
					if(resident.hasNation() && effectiveNation.getTowns().contains(resident.getTown())) {
						effectiveNumOnlinePlayers++;
					}
				} catch (NotRegisteredException ignored) {}
			}
		}
		event.setDisplayedValue(effectiveNumOnlinePlayers);
	}
}
