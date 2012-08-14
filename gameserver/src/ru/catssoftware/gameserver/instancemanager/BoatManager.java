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
package ru.catssoftware.gameserver.instancemanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.log4j.Logger;


import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.xml.XMLDocument;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance.L2BoatTrajet;
import ru.catssoftware.gameserver.templates.chars.L2CharTemplate;
import ru.catssoftware.util.StatsSet;


public class BoatManager extends XMLDocument
{
	private final static Logger	_log	= Logger.getLogger(BoatManager.class.getName());

	private static BoatManager	_instance;

	public static final BoatManager getInstance()
	{
		if (_instance == null)
			_instance = new BoatManager();
		
		return _instance;
	}

	// =========================================================
	// Data Field
	private Map<Integer, L2BoatInstance>	_staticItems	= new FastMap<Integer, L2BoatInstance>();
	@SuppressWarnings("unused")
	private boolean							_initialized;

	// =========================================================
	// Constructor
	public BoatManager()
	{
		_log.info("Initializing BoatManager");
		load();
	}

	// =========================================================
	// Method - Private
	private final void load()
	{
		_initialized = true;
		if (!Config.ALLOW_BOAT)
		{
			_initialized = false;
			return;
		}
		try
		{
			File f = new File(Config.DATAPACK_ROOT,"data/boat.xml");
			load(f);
		}
		catch (FileNotFoundException e)
		{
			_initialized = false;
			_log.warn("boat.xml is missing in data folder");
		}
		catch (Exception e)
		{
			_initialized = false;
			_log.warn("error while creating boat table ", e);
		}
	}

	@Override
	protected void parseDocument(Document doc)
	{
		for(Node n = doc.getFirstChild();n!=null;n=n.getNextSibling())
		{
			if(n.getNodeName().equals("list"))
			{
				for(Node object = n.getFirstChild();object!=null;object = object.getNextSibling())
				{
					if(object.getNodeName().equals("boat"))
					{
						L2BoatInstance boat = parseNode(object);
						boat.spawn();
						_staticItems.put(boat.getObjectId(), boat);
					}
				}
			}
		}
	}
	
	/**
	 * @param line
	 * @return
	 * @throws IOException 
	 */
	public L2BoatInstance parseNode(Node node)
	{
		L2BoatInstance boat;

		int id = Integer.parseInt(node.getAttributes().getNamedItem("id").getNodeValue());
		String name = node.getAttributes().getNamedItem("name").getNodeValue();

		int xspawn, yspawn, zspawn, heading;
		xspawn = yspawn = zspawn = heading = 0;
		int IdWaypoint_1, IdWTicket_1, ntx_1, nty_1, ntz_1;
		IdWaypoint_1 = IdWTicket_1 = ntx_1 = nty_1 = ntz_1 = 0;
		String mess10_1, mess5_1, mess1_1, mess0_1, messb_1, title_1;
		mess10_1 = mess5_1 = mess1_1 = mess0_1 = messb_1 = title_1 = "";

		L2BoatTrajet t1, t2;
		t1 = t2 = null;

		StatsSet npcDat = new StatsSet();
		npcDat.set("npcId", id);
		npcDat.set("level", 0);
		npcDat.set("jClass", "boat");

		npcDat.set("baseSTR", 0);
		npcDat.set("baseCON", 0);
		npcDat.set("baseDEX", 0);
		npcDat.set("baseINT", 0);
		npcDat.set("baseWIT", 0);
		npcDat.set("baseMEN", 0);

		npcDat.set("baseShldDef", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseAccCombat", 38);
		npcDat.set("baseEvasRate", 38);
		npcDat.set("baseCritRate", 38);

		npcDat.set("collision_radius", 0);
		npcDat.set("collision_height", 0);
		npcDat.set("fcollision_radius", 0);
		npcDat.set("fcollision_height", 0);
		npcDat.set("sex", "male");
		npcDat.set("type", "");
		npcDat.set("baseAtkRange", 0);
		npcDat.set("baseMpMax", 0);
		npcDat.set("baseCpMax", 0);
		npcDat.set("rewardExp", 0);
		npcDat.set("rewardSp", 0);
		npcDat.set("basePAtk", 0);
		npcDat.set("baseMAtk", 0);
		npcDat.set("basePAtkSpd", 0);
		npcDat.set("aggroRange", 0);
		npcDat.set("baseMAtkSpd", 0);
		npcDat.set("rhand", 0);
		npcDat.set("lhand", 0);
		npcDat.set("armor", 0);
		npcDat.set("baseWalkSpd", 0);
		npcDat.set("baseRunSpd", 800);
		npcDat.set("name", name);
		npcDat.set("baseHpMax", 50000);
		npcDat.set("baseHpReg", 3.e-3f);
		npcDat.set("baseMpReg", 3.e-3f);
		npcDat.set("basePDef", 100);
		npcDat.set("baseMDef", 100);

		L2CharTemplate template = new L2CharTemplate(npcDat);

		boat = new L2BoatInstance(IdFactory.getInstance().getNextId(), template, name);
		boat.setId(id);

		for(Node n = node.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(n.getNodeName().equals("spawn")) {
				xspawn = Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue());
				yspawn = Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue());
				zspawn = Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue());
				heading = Integer.parseInt(n.getAttributes().getNamedItem("heading").getNodeValue());
				boat.getPosition().setHeading(heading);
				boat.getPosition().setXYZ(xspawn, yspawn, zspawn);
			}
			else if(n.getNodeName().equals("ports")) {
				for(Node n1 = n.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
					if(n1.getNodeName().equals("port")) {
						IdWaypoint_1 = Integer.parseInt(n1.getAttributes().getNamedItem("waypoint").getNodeValue());
						IdWTicket_1 = Integer.parseInt(n1.getAttributes().getNamedItem("ticket").getNodeValue());
						ntx_1 = Integer.parseInt(n1.getAttributes().getNamedItem("telenoticketX").getNodeValue());
						nty_1 = Integer.parseInt(n1.getAttributes().getNamedItem("telenoticketY").getNodeValue());
						ntz_1 = Integer.parseInt(n1.getAttributes().getNamedItem("telenoticketZ").getNodeValue());
						for(Node n2 = n1.getFirstChild();n2!=null;n2=n2.getNextSibling()) {
							if(n2.getNodeName().equals("announcer"))
								title_1 = n2.getAttributes().getNamedItem("title").getNodeValue();
							else if(n2.getNodeName().equals("message10"))
								mess10_1 = n2.getAttributes().getNamedItem("text").getNodeValue();
							else if(n2.getNodeName().equals("message5"))
								mess5_1 = n2.getAttributes().getNamedItem("text").getNodeValue();
							else if(n2.getNodeName().equals("message1"))
								mess1_1 = n2.getAttributes().getNamedItem("text").getNodeValue();
							else if(n2.getNodeName().equals("message0"))
								mess0_1 = n2.getAttributes().getNamedItem("text").getNodeValue();
							else if(n2.getNodeName().equals("messageb"))
								messb_1 = n2.getAttributes().getNamedItem("text").getNodeValue();
						}
						if(t1==null)
							t1 = boat.setTrajet1(IdWaypoint_1, IdWTicket_1, ntx_1, nty_1, ntz_1, title_1, mess10_1, mess5_1, mess1_1, mess0_1, messb_1);
						else 
							t2 = boat.setTrajet2(IdWaypoint_1, IdWTicket_1, ntx_1, nty_1, ntz_1, title_1, mess10_1, mess5_1, mess1_1, mess0_1, messb_1);
						
					}
				}
			}
			else if(n.getNodeName().equals("routes")) {
				for(Node n1 = n.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
					if(n1.getNodeName().equals("route"))
					{
						if(t1!=null && t1.getPathSize()==0)
							t1.loadBoatPath(n1);
						else 
							if(t2!=null) 
								t2.loadBoatPath(n1);
					}
				}
			}
		}
		

		return boat;
	}

	/**
	 * @param boatId
	 * @return
	 */
	public L2BoatInstance getBoat(int boatId)
	{
		if (_staticItems == null)
			_staticItems = new FastMap<Integer, L2BoatInstance>();
		return _staticItems.get(boatId);
	}

	public Map<Integer, L2BoatInstance> getBoats()
	{
		if (_staticItems == null)
			_staticItems = new FastMap<Integer, L2BoatInstance>();
		return _staticItems;
	}
}
