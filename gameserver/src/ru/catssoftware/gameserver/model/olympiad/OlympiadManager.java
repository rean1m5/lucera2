/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.model.olympiad;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.Message.MessageId;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.olympiad.Olympiad.COMP_TYPE;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.tools.random.Rnd;

import java.util.Map;
import java.util.logging.Logger;


/**
 * @author GodKratos
 */
public class OlympiadManager implements Runnable
{
	protected static final Logger				_log		= Logger.getLogger(OlympiadManager.class.getName());
	private Map<Integer, OlympiadGame>			_olympiadInstances;
	protected static OlympiadManager			_instance;

	protected static final OlympiadStadium[]	STADIUMS	= {
			new OlympiadStadium(-120324, -225077, -3331),
			new OlympiadStadium(-102495, -209023, -3331),
			new OlympiadStadium(-120156, -207378, -3331),
			new OlympiadStadium(-87628, -225021, -3331),
			new OlympiadStadium(-81705, -213209, -3331),
			new OlympiadStadium(-87593, -207339, -3331),
			new OlympiadStadium(-93709, -218304, -3331),
			new OlympiadStadium(-77157, -218608, -3331),
			new OlympiadStadium(-69682, -209027, -3331),
			new OlympiadStadium(-76887, -201256, -3331),
			new OlympiadStadium(-109985, -218701, -3331),
			new OlympiadStadium(-126367, -218228, -3331),
			new OlympiadStadium(-109629, -201292, -3331),
			new OlympiadStadium(-87523, -240169, -3331),
			new OlympiadStadium(-81748, -245950, -3331),
			new OlympiadStadium(-77123, -251473, -3331),
			new OlympiadStadium(-69778, -241801, -3331),
			new OlympiadStadium(-76754, -234014, -3331),
			new OlympiadStadium(-93742, -251032, -3331),
			new OlympiadStadium(-87466, -257752, -3331),
			new OlympiadStadium(-114413, -213241, -3331)};

	public OlympiadManager()
	{
		_olympiadInstances = new FastMap<Integer, OlympiadGame>();
		_instance = this;
	}

	public static OlympiadManager getInstance()
	{
		if (_instance == null)
			_instance = new OlympiadManager();

		return _instance;
	}

	public synchronized void run()
	{
		if (Olympiad.getInstance().isOlympiadEnd())
			return;

		Map<Integer, OlympiadGameTask> _gamesQueue = new FastMap<Integer, OlympiadGameTask>();
		while (Olympiad.getInstance().inCompPeriod())
		{
			if (Olympiad.getNobleCount() == 0)
			{
				try
				{
					wait(60000);
				}
				catch (InterruptedException ex)
				{
				}
				continue;
			}

			int _gamesQueueSize = 0;

			// _compStarted = true;
			FastList<Integer> readyClasses = Olympiad.hasEnoughRegisteredClassed();
			boolean readyNonClassed = Olympiad.hasEnoughRegisteredNonClassed();
			if (readyClasses != null || readyNonClassed)
			{
				// set up the games queue
				for (int i = 0; i < STADIUMS.length; i++)
				{
					if (!existNextOpponents(Olympiad.getRegisteredNonClassBased()) && !existNextOpponents(getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses)))
						break;
					if (STADIUMS[i].isFreeToUse())
					{
						if (i < STADIUMS.length / 2)
						{
							if (readyNonClassed && existNextOpponents(Olympiad.getRegisteredNonClassBased()))
							{
								try
								{
									_olympiadInstances.put(i, new OlympiadGame(i, COMP_TYPE.NON_CLASSED, nextOpponents(Olympiad.getRegisteredNonClassBased())));
									_gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i)));
									STADIUMS[i].setStadiaBusy();
								}
								catch (Exception ex)
								{
									if (_olympiadInstances.get(i) != null)
									{
										for (L2PcInstance player : _olympiadInstances.get(i).getPlayers())
										{
											player.sendMessage(Message.getMessage(player, MessageId.MSG_REG_FAIL));
											player.setIsInOlympiadMode(false);
											enchantUpdate(player);
											player.setIsOlympiadStart(false);
											player.setOlympiadSide(-1);
											player.setOlympiadGameId(-1);
										}
										_olympiadInstances.remove(i);
									}
									if (_gamesQueue.get(i) != null)
										_gamesQueue.remove(i);
									STADIUMS[i].setStadiaFree();

									// try to reuse this stadia next time
									i--;
								}
							}

							else if (readyClasses != null && existNextOpponents(getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses)))
							{
								try
								{
									_olympiadInstances.put(i, new OlympiadGame(i, COMP_TYPE.CLASSED, nextOpponents(getRandomClassList(Olympiad
											.getRegisteredClassBased(), readyClasses))));
									_gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i)));
									STADIUMS[i].setStadiaBusy();
								}
								catch (Exception ex)
								{
									if (_olympiadInstances.get(i) != null)
									{
										for (L2PcInstance player : _olympiadInstances.get(i).getPlayers())
										{
											player.sendMessage(Message.getMessage(player, MessageId.MSG_REG_FAIL));
											player.setIsInOlympiadMode(false);
											player.setIsOlympiadStart(false);
											player.setOlympiadSide(-1);
											player.setOlympiadGameId(-1);
											enchantUpdate(player);
										}
										_olympiadInstances.remove(i);
									}
									if (_gamesQueue.get(i) != null)
										_gamesQueue.remove(i);
									STADIUMS[i].setStadiaFree();

									// try to reuse this stadia next time
									i--;
								}
							}
						}
						else
						{
							if (readyClasses != null && existNextOpponents(getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses)))
							{
								try
								{
									_olympiadInstances.put(i, new OlympiadGame(i, COMP_TYPE.CLASSED, nextOpponents(getRandomClassList(Olympiad
											.getRegisteredClassBased(), readyClasses))));
									_gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i)));
									STADIUMS[i].setStadiaBusy();
								}
								catch (Exception ex)
								{
									if (_olympiadInstances.get(i) != null)
									{
										for (L2PcInstance player : _olympiadInstances.get(i).getPlayers())
										{
											player.sendMessage(Message.getMessage(player, MessageId.MSG_REG_FAIL));
											player.setIsInOlympiadMode(false);
											player.setIsOlympiadStart(false);
											player.setOlympiadSide(-1);
											player.setOlympiadGameId(-1);
											enchantUpdate(player);
										}
										_olympiadInstances.remove(i);
									}
									if (_gamesQueue.get(i) != null)
										_gamesQueue.remove(i);
									STADIUMS[i].setStadiaFree();

									// try to reuse this stadia next time
									i--;
								}
							}
							else if (readyNonClassed && existNextOpponents(Olympiad.getRegisteredNonClassBased()))
							{
								try
								{
									_olympiadInstances.put(i, new OlympiadGame(i, COMP_TYPE.NON_CLASSED, nextOpponents(Olympiad.getRegisteredNonClassBased())));
									_gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i)));
									STADIUMS[i].setStadiaBusy();
								}
								catch (Exception ex)
								{
									if (_olympiadInstances.get(i) != null)
									{
										for (L2PcInstance player : _olympiadInstances.get(i).getPlayers())
										{
											player.sendMessage(Message.getMessage(player, MessageId.MSG_REG_FAIL));
											player.setIsInOlympiadMode(false);
											player.setIsOlympiadStart(false);
											player.setOlympiadSide(-1);
											player.setOlympiadGameId(-1);
											enchantUpdate(player);
										}
										_olympiadInstances.remove(i);
									}
									if (_gamesQueue.get(i) != null)
										_gamesQueue.remove(i);
									STADIUMS[i].setStadiaFree();

									// try to reuse this stadia next time
									i--;
								}
							}
						}
					}
					else
					{
						if (_gamesQueue.get(i) == null || _gamesQueue.get(i).isTerminated() || _gamesQueue.get(i)._game == null)
						{
							// removes terminated games from the queue
							try
							{
								_olympiadInstances.remove(i);
								_gamesQueue.remove(i);
								STADIUMS[i].setStadiaFree();
								i--;
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				}

				// Start games
				_gamesQueueSize = _gamesQueue.size();
				for (int i = 0; i < _gamesQueueSize; i++)
				{
					if (_gamesQueue.get(i) != null && !_gamesQueue.get(i).isTerminated() && !_gamesQueue.get(i).isStarted())
					{
						// start new games
						Thread T = new Thread(_gamesQueue.get(i));
						T.start();
					}

					// Pause one second between games starting to reduce OlympiadManager shout spam.
					try
					{
						wait(1000);
					}
					catch (InterruptedException e)
					{
					}
				}
			}

			// wait 30 sec for !stress the server
			try
			{
				wait(30000);
			}
			catch (InterruptedException e)
			{
			}
		}

		// when comp time finish wait for all games terminated before execute
		// the cleanup code
		boolean allGamesTerminated = false;
		// wait for all games terminated
		while (!allGamesTerminated)
		{
			try
			{
				wait(30000);
			}
			catch (InterruptedException e)
			{
			}

			if (_gamesQueue.size() == 0)
				allGamesTerminated = true;
			else
			{
				for (OlympiadGameTask game : _gamesQueue.values())
					allGamesTerminated = allGamesTerminated || game.isTerminated();
			}
		}
		// when all games terminated clear all
		_gamesQueue.clear();
		_olympiadInstances.clear();
		Olympiad.clearRegistered();

		OlympiadGame._battleStarted = false;
	}

	protected OlympiadGame getOlympiadGame(int index)
	{
		if (_olympiadInstances != null && !_olympiadInstances.isEmpty())
			return _olympiadInstances.get(index);

		return null;
	}

	protected void removeGame(OlympiadGame game)
	{
		if (_olympiadInstances != null && !_olympiadInstances.isEmpty())
		{
			for (int i = 0; i < _olympiadInstances.size(); i++)
			{
				if (_olympiadInstances.get(i) == game)
					_olympiadInstances.remove(i);
			}
		}
	}

	protected Map<Integer, OlympiadGame> getOlympiadGames()
	{
		return (_olympiadInstances == null) ? null : _olympiadInstances;
	}

	protected FastList<L2PcInstance> getRandomClassList(Map<Integer, FastList<L2PcInstance>> list, FastList<Integer> classList)
	{
		if (list == null || classList == null || list.size() == 0 || classList.size() == 0)
			return null;

		return list.get(classList.get(Rnd.nextInt(classList.size())));
	}

	protected FastList<L2PcInstance> nextOpponents(FastList<L2PcInstance> list)
	{
		FastList<L2PcInstance> opponents = new FastList<L2PcInstance>();
		if (list.size() == 0)
			return opponents;
		int loopCount = (list.size() / 2);

		int first;
		int second;

		if (loopCount < 1)
			return opponents;

		first = Rnd.get(list.size());
		opponents.add(list.get(first));
		list.remove(first);

		second = Rnd.get(list.size());
		opponents.add(list.get(second));
		list.remove(second);

		return opponents;

	}

	protected boolean existNextOpponents(FastList<L2PcInstance> list)
	{
		if (list == null)
			return false;
		if (list.size() == 0)
			return false;
		int loopCount = list.size() >> 1;

		if (loopCount < 1)
			return false;
		else
			return true;

	}

	public FastMap<Integer, String> getAllTitles()
	{
		FastMap<Integer, String> titles = new FastMap<Integer, String>();

		for (OlympiadGame instance : _olympiadInstances.values())
		{
			if (instance == null || !instance._gamestarted)
				continue;
			titles.put(instance._stadiumID, instance.getTitle());
		}
		return titles;
	}

	public static void enchantUpdate(L2PcInstance player)
	{
		if (Config.ALT_OLY_ENCHANT_LIMIT < 0)
			return;

		InventoryUpdate inventoryUpdate = new InventoryUpdate();
		for(L2ItemInstance item : player.getInventory().getItems())
			if (item.getEnchantLevel() > Config.ALT_OLY_ENCHANT_LIMIT)
				inventoryUpdate.addModifiedItem(item);
		player.sendPacket(inventoryUpdate);
		player.broadcastUserInfo(true);
	}
}