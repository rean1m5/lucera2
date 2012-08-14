package ru.catssoftware.gameserver.cache;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

import javolution.util.FastMap;


public class HTMParser {

	private static Pattern _IfFinder = Pattern.compile("<!--if +(.+) +-->(.+)<!--/if-->");
	private static Pattern _ElseFinder = Pattern.compile("(.+)<!--else-->(.+)");
	private static Pattern _IncludeFinder = Pattern.compile("<!--include +(.+) +-->");
	public static class HTMLTable {
		private int _numCols;
		private int _curCol = 1;
		private String result = "<tr>";
		public HTMLTable(int numCols) {
			_numCols = numCols;
			if(_numCols<1) _numCols = 1;
		}
		public void add(String td, String behavor) {
			if(_curCol>_numCols) {
				result+="</tr><tr>";
				_curCol = 1;
			}
			
			result+="<td";
			if(behavor!=null)
				result+=(" "+behavor);
			result+=">"+td+"</td>";
			_curCol++;	
		}
		public void add(String td) {
			add(td,null);
		}
		
		public String getTable() {
			while(_curCol++ <= _numCols) result+="<td></td>";
			result+="</tr>";
			return result;
		}
	}
	
	private static class HTMCondition {
		private String _html = "";
		private String _else = "";
		private ICondition _cond;
		public HTMCondition(String check, String html) {
			_cond = DynamicCondition.getHTMLDynamicCondition(check);
			Matcher m = _ElseFinder.matcher(html);
			if(m.find()) {
				_html = m.group(1);
				_else = m.group(2);
			} else
				_html = html;
		}
		public boolean isOk(Object...params) {
			if(_cond!=null)
				return _cond.isValid(params);
			return false;
		}
	}
	private static class IncludeResult {
		String htm;
		boolean isOk;
	}
	private static IncludeResult parseInclude(String html,L2PcInstance player) {
		IncludeResult result = new IncludeResult();
		result.htm = html;
		Matcher m = _IncludeFinder.matcher(result.htm);
		while(m.find()) {
			result.isOk = true;
			String htm = HtmCache.getInstance().getHtm(m.group(1),player);
			result.htm = m.replaceFirst(htm!=null ? htm : "");
			m = _IncludeFinder.matcher(result.htm);
		}
		return result;
	}
	public static String parseHTM(String html, Object...params) {
		if(html==null)
			return null;
		L2PcInstance player = null;
		for(int i=0;i<params.length;i++)
			if(params[i] instanceof L2PcInstance) {
				player = (L2PcInstance)params[i];
				break;
			}
		IncludeResult ir = parseInclude(html,player);
		while(ir.isOk) 
			ir = parseInclude(ir.htm,player);
		
		html = ir.htm;
		Map<String, HTMCondition> conditions = new FastMap<String, HTMCondition>();
		Matcher m = _IfFinder.matcher(html);
		int cNum = 1;
		while(m.find()) {
			String cName= "<!--COND"+(cNum++)+"-->";
			conditions.put(cName, new HTMCondition(m.group(1),m.group(2)));
			
			html = m.replaceFirst(cName);
			m = _IfFinder.matcher(html);
		}
			
		for(String s : conditions.keySet()) {
			HTMCondition c = conditions.get(s);
			if(c.isOk(params))
				html = html.replace(s, c._html);
			else 
				html = html.replace(s, c._else);
		}
		return html;
	}
}
