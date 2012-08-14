package ru.catssoftware.gameserver.datatables.xml;

import java.io.File;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.xml.XMLDocument;

public class NpcLikePcTemplates extends XMLDocument {

	private static Logger _log = Logger.getLogger(NpcLikePcTemplate.class);
	private static NpcLikePcTemplates _instance;
	public static NpcLikePcTemplates getInstance() {
		if(_instance==null)
			_instance = new NpcLikePcTemplates();
		return _instance;
	}
	private Map<Integer, NpcLikePcTemplate > _templates = new FastMap<Integer, NpcLikePcTemplate>();
	private NpcLikePcTemplates() {
		try {
			
			load(new File(Config.DATAPACK_ROOT,"data/extensions/npclikepc.xml"));
			_log.info("NpcLikePc :Loaded "+_templates.size()+" template(s)");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
 	@Override
	protected void parseDocument(Document doc) {
		for(Node n = doc.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(n.getNodeName().equals("list")) {
				for(Node d = n.getFirstChild();d!=null;d=d.getNextSibling()) {
					if(d.getNodeName().equals("npc")) {
						int id= Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
						NpcLikePcTemplate tpl = new NpcLikePcTemplate(d);
						_templates.put(id, tpl);
					}
				}
			}
		}
	}
	public NpcLikePcTemplate getTemplate(int npcId) {
		return _templates.get(npcId);
	}

}
