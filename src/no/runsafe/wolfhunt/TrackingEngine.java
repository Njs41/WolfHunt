package no.runsafe.wolfhunt;

import net.minecraft.server.v1_7_R2.EntityWolf;
import net.minecraft.server.v1_7_R2.PathEntity;
import no.runsafe.framework.api.ILocation;
import no.runsafe.framework.api.event.player.IPlayerInteractEntityEvent;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.internal.wrapper.ObjectUnwrapper;
import no.runsafe.framework.minecraft.Sound;
import no.runsafe.framework.minecraft.entity.LivingEntity;
import no.runsafe.framework.minecraft.entity.RunsafeEntity;
import no.runsafe.framework.minecraft.event.player.RunsafePlayerInteractEntityEvent;
import no.runsafe.framework.minecraft.item.meta.RunsafeMeta;

import java.util.List;

public class TrackingEngine implements IPlayerInteractEntityEvent
{
	public TrackingEngine(Config config)
	{
		this.config = config;
	}

	@Override
	public void OnPlayerInteractEntityEvent(RunsafePlayerInteractEntityEvent event)
	{
		RunsafeEntity entity = event.getRightClicked();

		if (entity != null && entity.getEntityType() == LivingEntity.Wolf)
		{
			EntityWolf wolf = (EntityWolf) ObjectUnwrapper.getMinecraft(entity);
			if (wolf == null || wolf.isBaby())
				return;

			IPlayer wolfOwner = event.getPlayer();
			if (wolfOwner.getName().equals(wolf.getOwnerName()) && isHoldingTrackingItem(wolfOwner))
			{
				ILocation wolfLocation = entity.getLocation();
				if (wolfLocation != null)
				{
					List<IPlayer> worldPlayers = wolfLocation.getWorld().getPlayers();
					if (!worldPlayers.isEmpty())
					{
						double closestPlayerDist = 0;
						ILocation closetPlayerLoc = null;
						IPlayer closestPlayer = null;

						for (IPlayer worldPlayer : worldPlayers)
						{
							if (worldPlayer.isCreative() || wolfOwner.shouldNotSee(worldPlayer) || wolfOwner.getName().equals(worldPlayer.getName()))
								continue;

							ILocation worldPlayerLocation = worldPlayer.getLocation();
							if (worldPlayerLocation != null)
							{
								double distance = worldPlayerLocation.distance(wolfLocation);
								if (closestPlayer == null || distance < closestPlayerDist)
								{
									closestPlayer = worldPlayer;
									closestPlayerDist = distance;
									closetPlayerLoc = worldPlayerLocation;
								}
							}
						}

						if (closestPlayer != null)
						{
							// Make the wolf growl and begin the tracking
							wolfLocation.playSound(Sound.Creature.Wolf.Growl, 1, 1);
							wolf.setSitting(false);
							PathEntity path = wolf.world.a(wolf, closetPlayerLoc.getBlockX(),  closetPlayerLoc.getBlockY(), closetPlayerLoc.getBlockZ(), 16.0f, true, false, false, true);
							wolf.setPathEntity(path);
						}
						else
						{
							// No valid players, bark!
							wolfLocation.playSound(Sound.Creature.Wolf.Bark, 1, 1);
						}
					}
					else
					{
						// No players at all. Not sure how this even happens. Just bark.
						wolfLocation.playSound(Sound.Creature.Wolf.Bark, 1, 1);
					}
					event.cancel();
				}
			}
		}
	}

	private boolean isHoldingTrackingItem(IPlayer player)
	{
		RunsafeMeta heldItem = player.getItemInHand();
		return heldItem != null && heldItem.is(config.getTrackingItem());
	}

	private final Config config;
}
