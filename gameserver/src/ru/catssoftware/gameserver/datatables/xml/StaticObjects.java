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
package ru.catssoftware.gameserver.datatables.xml;

import java.io.File;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastMap;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.actor.instance.L2StaticObjectInstance;
import ru.catssoftware.gameserver.templates.chars.L2CharTemplate;
import ru.catssoftware.util.StatsSet;

public class StaticObjects extends XMLDocument
{
	private final static Logger							_log	= Logger.getLogger(StaticObjects.class.getName());

	private static StaticObjects						_instance;
	private Map<Integer, L2StaticObjectInstance>		_staticObjects;

	public static StaticObjects getInstance()
	{
		if (_instance == null)
			_instance = new StaticObjects();
		return _instance;
	}

	public StaticObjects()
	{
		_staticObjects = new FastMap<Integer, L2StaticObjectInstance>();
		parseData();
	}

	private void parseData()
	{
		try
		{
			File f = new File(Config.DATAPACK_ROOT,"data/staticobjects.xml");
			load(f);
			_log.info("Static Objects: Loaded "+_staticObjects.size()+" object(s)");
		}
		catch (Exception e)
		{
			_log.warn("Static Objects: Error reading staticobjects.xml",e);
		}
	}

	public static L2StaticObjectInstance parse(String line)
	{
		StringTokenizer st = new StringTokenizer(line, ";");

		st.nextToken(); //Pass over static object name (not used in server)

		int id = Integer.parseInt(st.nextToken());
		int x = Integer.parseInt(st.nextToken());
		int y = Integer.parseInt(st.nextToken());
		int z = Integer.parseInt(st.nextToken());
		int type = Integer.parseInt(st.nextToken());
		String texture = st.nextToken();
		int map_x = Integer.parseInt(st.nextToken());
		int map_y = Integer.parseInt(st.nextToken());

		StatsSet npcDat = new StatsSet();
		npcDat.set("npcId", id);
		npcDat.set("level", 0);

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

		npcDat.set("collision_radius", 10);
		npcDat.set("collision_height", 10);
		npcDat.set("fcollision_radius", 10);
		npcDat.set("fcollision_height", 10);
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
		npcDat.set("baseRunSpd", 0);
		npcDat.set("name", "");
		npcDat.set("baseHpMax", 1);
		npcDat.set("baseHpReg", 3.e-3f);
		npcDat.set("baseMpReg", 3.e-3f);
		npcDat.set("basePDef", 1);
		npcDat.set("baseMDef", 1);

		L2CharTemplate template = new L2CharTemplate(npcDat);
		L2StaticObjectInstance obj = new L2StaticObjectInstance(IdFactory.getInstance().getNextId(), template, id);

		obj.setType(type);
		obj.getPosition().setXYZ(x, y, z);
		obj.setMap(texture, map_x, map_y);
		obj.spawnMe();

		return obj;
	}
	
	public L2StaticObjectInstance parseNode(Node node)
	{

		int id = Integer.parseInt(node.getAttributes().getNamedItem("id").getNodeValue());
		int type = Integer.parseInt(node.getAttributes().getNamedItem("type").getNodeValue());
		String texture = node.getAttributes().getNamedItem("texture").getNodeValue();
		int x = 0;
		int y = 0;
		int z = 0;
		int map_x = 0;
		int map_y = 0;
		for(Node n = node.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(n.getNodeName().equals("position")) {
				x = Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue());
				y = Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue());
				z = Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue());
				map_x = Integer.parseInt(n.getAttributes().getNamedItem("map_x").getNodeValue());
				map_y = Integer.parseInt(n.getAttributes().getNamedItem("map_y").getNodeValue());
			}
		}
		StatsSet npcDat = new StatsSet();
		npcDat.set("npcId", id);
		npcDat.set("level", 0);
		npcDat.set("jClass", "staticobject");

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

		npcDat.set("collision_radius", 10);
		npcDat.set("collision_height", 10);
		npcDat.set("fcollision_radius", 10);
		npcDat.set("fcollision_height", 10);
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
		npcDat.set("baseRunSpd", 0);
		npcDat.set("name", "");
		npcDat.set("baseHpMax", 1);
		npcDat.set("baseHpReg", 3.e-3f);
		npcDat.set("baseMpReg", 3.e-3f);
		npcDat.set("basePDef", 1);
		npcDat.set("baseMDef", 1);

		L2CharTemplate template = new L2CharTemplate(npcDat);
		L2StaticObjectInstance obj = new L2StaticObjectInstance(IdFactory.getInstance().getNextId(), template, id);

		obj.setType(type);
		obj.getPosition().setXYZ(x, y, z);
		obj.setMap(texture, map_x, map_y);
		obj.spawnMe();

		return obj;
	}

	public void putObject(L2StaticObjectInstance obj)
	{
		_staticObjects.put(obj.getStaticObjectId(), obj);
	}

	@Override
	protected void parseDocument(Document doc) {
		for(Node n = doc.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(n.getNodeName().equals("list")) {
				for(Node object = n.getFirstChild();object!=null;object = object.getNextSibling()) {
					if(object.getNodeName().equals("object")) {
						L2StaticObjectInstance obj = parseNode(object);
						_staticObjects.put(obj.getStaticObjectId(), obj);
						
					}
				}
			}
		}
		
	}
	

}