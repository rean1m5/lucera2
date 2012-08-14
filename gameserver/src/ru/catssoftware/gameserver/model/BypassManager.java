package ru.catssoftware.gameserver.model;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.util.Strings;
import ru.catssoftware.tools.random.Rnd;


public class BypassManager
{

	private static final Pattern p = Pattern.compile("\"(bypass +-h +)(.+?)\"");
	private static Logger _log = Logger.getLogger(BypassManager.class); 
	public static enum BypassType
	{
		ENCODED,
		ENCODED_BBS,
		SIMPLE,
		SIMPLE_BBS,
		SIMPLE_DIRECT
	}

	public static BypassType getBypassType(String bypass)
	{
		switch(bypass.charAt(0))
		{
			case '0':
				return BypassType.ENCODED;
			case '1':
				return BypassType.ENCODED_BBS;
			default:
				if(Strings.matches(bypass, "^(_mrsl|_clbbs|_mm|_diary|friendlist|friendmail|manor_menu_select|_match).*", Pattern.DOTALL))
					return BypassType.SIMPLE;
				if(Strings.matches(bypass, "^(bbs_|_bbs|_mail|_friend|_block).*", Pattern.DOTALL))
					return BypassType.SIMPLE_BBS;
				return BypassType.SIMPLE_DIRECT;
		}
	}

	public static String encode(String html, Map<String,EncodedBypass> bypassStorage, boolean bbs)
	{
		if(html==null)
			return null;
		Matcher m = p.matcher(html);
		StringBuffer sb = new StringBuffer();
		bypassStorage.clear();
		while(m.find())
		{
			
			String bypass = m.group(2);
			String code = bypass;
			String params = "";
			int i = bypass.indexOf(" $");
			boolean use_params = i >= 0;
			if(use_params)
			{
				code = bypass.substring(0, i);
				params = bypass.substring(i).replace("$", "\\$");
			}

			String key = String.valueOf(Rnd.get(500000));
			while (bypassStorage.containsKey(key))
				key = String.valueOf(Rnd.get(500000));
			if(bbs)
				m.appendReplacement(sb, "\"bypass -h 1" + key + params + "\"");
			else
				m.appendReplacement(sb, "\"bypass -h 0" + key + params + "\"");

			bypassStorage.put(key,new EncodedBypass(code, use_params));
		}

		m.appendTail(sb);
		return sb.toString();
	}

	public static DecodedBypass decode(String bypass, Map<String,EncodedBypass> bypassStorage, boolean bbs, L2PcInstance player)
	{
		synchronized (bypassStorage)
		{
			String[] bypass_parsed = bypass.split(" ");
			String idx = bypass_parsed[0].substring(1).trim();
			EncodedBypass bp;
			DecodedBypass result = null;

			try
			{
				bp = bypassStorage.get(idx);
			}
			catch(Exception e)
			{
				bp = null;
			}

			if(bp == null)
				_log.warn("BypasManager: Can't decode bypass (bypass not exists): " + (bbs ? "[bbs] " : "") + bypass + " / Player: " + player.getName());
			else if(bypass_parsed.length > 1 && !bp.useParams)
				_log.warn("BypasManager: bypass with wrong params" + (bbs ? " [bbs]: " : ": ") + bp.code + " / Player: " + player.getName());
			else
			{
				result = new DecodedBypass(bp.code, bbs);
				for(int i = 1; i < bypass_parsed.length; i++)
					result.bypass += " " + bypass_parsed[i];
				result.trim();
			}

			return result;
		}
	}

	public static class EncodedBypass
	{
		public final String code;
		public final boolean useParams;

		public EncodedBypass(String _code, boolean _useParams)
		{
			code = _code;
			useParams = _useParams;
		}
	}

	public static class DecodedBypass
	{
		public String bypass;
		public boolean bbs;

		public DecodedBypass(String _bypass, boolean _bbs)
		{
			bypass = _bypass;
			bbs = _bbs;
		}

		public DecodedBypass trim()
		{
			bypass = bypass.trim();
			return this;
		}
	}
}
