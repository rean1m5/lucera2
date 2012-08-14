package ru.catssoftware.gameserver.cache;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.util.Util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HtmCache
{
	private static Logger _log = Logger.getLogger("GAME");
	private static HtmCache _instance;
	private File HTM_ROOT = new File(Config.DATAPACK_ROOT,"data/html");
	private List<IHtmLoad> _listHtmlCacheScripts;
	private int _loadedFiles;
	public static HtmCache getInstance() {
		if(_instance==null)
			_instance = new HtmCache();
		return _instance;
	}
	public static enum ISO639 {
		aa("Afar"),ae("Avestan"),am("Amharic"),ar("Arabic"),ay("Aymara"),ba("Bashkir"),bg("Bulgarian"),bi("Bislama"),bo("Tibetan"),bs("Bosnian"),ce("Chechen"),co("Corsican"),cu("Church Slavic; Slavonic; Old Bulgarian"),cy("Welsh"),de("German"),dz("Dzongkha"),en("English"),es("Spanish; Castilian"),eu("Basque"),fi("Finnish"),fo("Faroese"),fy("Western Frisian"),gd("Gaelic; Scottish Gaelic"),gn("Guarani"),gv("Manx"),he("Hebrew"),ho("Hiri Motu"),ht("Haitian; Haitian Creole "),hy("Armenian"),ia("Interlingua (International Auxiliary Language Association)"),ie("Interlingue"),ik("Inupiaq"),is("Icelandic"),iu("Inuktitut"),jv("Javanese"),ki("Kikuyu; Gikuyu"),kk("Kazakh"),km("Khmer"),ko("Korean"),ku("Kurdish"),kw("Cornish"),la("Latin"),li("Limburgan; Limburger; Limburgish"),lo("Lao"),lv("Latvian"),mh("Marshallese"),mk("Macedonian"),mn("Mongolian"),mr("Marathi"),mt("Maltese"),na("Nauru"),nd("Ndebele, North"),ng("Ndonga"),nn("Norwegian Nynorsk"),nr("Ndebele, South"),ny("Nyanja; Chichewa; Chewa"),om("Oromo"),os("Ossetian; Ossetic"),pi("Pali"),ps("Pushto"),qu("Quechua"),rn("Rundi"),ru("Russian"),sa("Sanskrit"),sd("Sindhi"),sg("Sango"),sk("Slovak"),sm("Samoan"),so("Somali"),sr("Serbian"),st("Sotho, Southern"),sv("Swedish"),ta("Tamil"),tg("Tajik"),ti("Tigrinya"),tl("Tagalog"),to("Tonga (Tonga Islands)"),ts("Tsonga"),tw("Twi"),ug("Uighur"),ur("Urdu"),vi("Vietnamese"),wa("Walloon"),xh("Xhosa"),yo("Yoruba"),zh("Chinese"), none("None");
		
		private String _langName;
		private ISO639(String langName) {
			_langName = langName;
		}
		public String getLanguageName() { return _langName; }
	}
	private abstract class HTMData {
		public abstract String getConent();
		protected String compactHtml(String html)
		{
			html = html.replace("  ", " ");
			html = html.replace("< ", "<");
			html = html.replace(" >", ">");
			html = html.replace("\r","");
			html = html.replace("\n\n", "\n").replace(">\n", ">").replace("\n<", "<");
			html = html.replace("<!--if","\n<!--if");
			return html.trim();
		}
		
	}

	private class HTMFile extends HTMData {
		private String _content = null;
		private File _file;
		public HTMFile(File file) {
			_file = file;
		}	
		@Override
		public String getConent() {
			if(_content==null) try {
				synchronized(this) {
					BufferedInputStream bis = new BufferedInputStream(new FileInputStream(_file));
					byte[] raw = new byte[bis.available()];
					bis.read(raw);
					_content = compactHtml(new String(raw, "UTF-8"));
					bis.close();
				}
				} catch(IOException ioe) {
					_log.error("Error reading file "+Util.getRelativePath(Config.DATAPACK_ROOT, _file)+": "+ioe.getMessage());
					_content = "<html><body><br>Error reading file <font color=\"LEVEL\">"+Util.getRelativePath(Config.DATAPACK_ROOT, _file)+"</font></body></html>";
				}
			return _content;
		}
	}
	private Map<ISO639 , Map<String,HTMData>> _htmls = new FastMap<ISO639, Map<String,HTMData>>();
	private List<String> _bugfiles = new FastList<String>();
	private HtmCache() {
		load();
	}
	
	public void load() {
		_listHtmlCacheScripts = new ArrayList<IHtmLoad>();
		_htmls.clear();
		_bugfiles.clear();
		_htmls.put(ISO639.none, new FastMap<String, HTMData>());
		_loadedFiles = 0;
		scanDir(HTM_ROOT);
		for(IHtmLoad script : _listHtmlCacheScripts)
			script.htmLoad();
	}

	public String toString() {
		return "HtmCache: Loaded "+_loadedFiles+" HTM file(s) for "+(_htmls.keySet().size()-1)+" language(s)";
				

	}
	public static ISO639 getPlayerLang(L2PcInstance player) {
		if(player==null || player.getClient() == null)
			return ISO639.en;
		return ISO639.valueOf(player.getClient().getAccountData().getString(L2GameClient.PLAYER_LANG,"en"));
	}
	
	public String getHtm(String file,L2PcInstance player) {
		file = file.replace("//", "/"); 
		ISO639 lng = getPlayerLang(player);
		Map<String,HTMData> list = _htmls.get(lng);
		if(list!=null && list.containsKey(file))
			return list.get(file).getConent();
		list = _htmls.get(ISO639.en);
		if(list!=null && list.containsKey(file))
			return list.get(file).getConent();
		list = _htmls.get(ISO639.none);
		if(list.containsKey(file))
			return list.get(file).getConent();
		if(Config.DEBUG && !_bugfiles.contains(file)) {
			_log.info("File `"+file+"` not exists");
			_bugfiles.add(file);
		} 
		return null;
	}
	private ISO639 _curLang = ISO639.none;
	public void scanDir(File dir) {
		if(dir.isDirectory()) {
			for(File f : dir.listFiles()) {
				if(f.isDirectory()) {
					try {
						_curLang = ISO639.valueOf(f.getName());
					} catch(Exception e) {
						_curLang = ISO639.none;
					}
					readFolder(f);
				} else if(f.getName().endsWith(".htm")) {
					_curLang = ISO639.none;
					loadFile(f);
				}
			}
		}
	}
	private void readFolder(File file) {
		for(File f : file.listFiles()) {
			if(f.getName().startsWith("."))
				continue;
			if(f.isDirectory())
				readFolder(f);
			else if(f.getName().endsWith(".htm")) 
				loadFile(f);
				 
				
		}
	}
	private void loadFile(File f) {
		String name = Util.getRelativePath(Config.DATAPACK_ROOT, f);
		name = name.replace(_curLang+"/", "");
		Map<String,HTMData> list = _htmls.get(_curLang);
		if(list==null) {
			list = new FastMap<String, HTMData>();
			_htmls.put(_curLang, list);
		}
		HTMFile htm = new HTMFile(f);
//		if(htm.getConent()==null)
//			continue;
		list.put(name, htm);	
		_loadedFiles++;
	}

	public Collection<ISO639> getLangs() {
		return _htmls.keySet();
	}


	public boolean pathExists(String temp) {
		for(Map<String,HTMData> v : _htmls.values())
			if(v.containsKey(temp))
				return true;
		return false;
	}

	public String getHtmForce(String file, L2PcInstance player) {
		return getHtm(file, player);
	}

	public String getQuestHtm(String fileName, Quest quest, L2PcInstance player) {
		String result = null;
		String questFolder = null;
		if(quest.getScriptFile()!=null) 
			questFolder = Util.getRelativePath(Config.DATAPACK_ROOT, new File(quest.getScriptFile()).getParentFile());
		else  {
			questFolder = "data/scripts/";
			if(quest.getQuestIntId()<0 || quest.getQuestIntId()> 1000)
				questFolder+="custom/";
			else 
				questFolder+="quests/";
			questFolder +=quest.getName();
		}
		result = getHtm((questFolder+"/"+fileName), player);
		if(result==null)
			result +="<html><body><br>File "+(questFolder+"/"+fileName)+" is missing</body></html>";
		return result; 
	}

	public void addHtmlCacheScript(IHtmLoad script)
	{
		if (!_listHtmlCacheScripts.contains(script))
		{
			_listHtmlCacheScripts.add(script);
			script.htmLoad();
		}
	}
}
