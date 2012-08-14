package ru.catssoftware.gameserver.cache;

import java.lang.reflect.Constructor;
import java.util.Map;


import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.apache.log4j.Logger;

import ru.catssoftware.gameserver.scripting.L2ScriptEngineManager;




import javolution.util.FastMap;

public class DynamicCondition  {
	private static Logger _log = Logger.getLogger("GAME");
	private static int _index = 1;
	private static Map<String , ICondition> _compiled = new FastMap<String, ICondition>().setShared(true);
	private static ScriptEngine _jsEngine = L2ScriptEngineManager.getInstance().getEngineByExtension("js");
	static {
		if(_jsEngine == null)
			_log.error("No ECMAScript engine found. Contitions are not possible!");
		
	}
	
	private static ICondition getDynamicCondition(String cond, Class<? extends BaseDynamicCondition> clazz) {
		if(_jsEngine==null)  {
			System.out.println("NO ENGINE!!!!");
			return null;
		}
		cond = cond.trim();
		ICondition result = _compiled.get(cond);
		if(result==null) try {
			Constructor<? extends BaseDynamicCondition > ctor = clazz.getConstructor(String.class);
			result = ctor.newInstance(cond);
			_compiled.put(cond, result);
		} catch(Exception e) {
			_log.error("Error creating "+clazz.getSimpleName(),e);
			result = null;
		}
		return result;
	}
	public static ICondition getHTMLDynamicCondition(String cond) {
		return getDynamicCondition(cond, HTMLDynamicCondition.class);
	}

	public static ICondition getSkillDynamicCondition(String cond) {
		return getDynamicCondition(cond, SkillDynamicCondition.class);
	}
	public static ICondition getEffectDynamicCondition(String cond) {
		return getDynamicCondition(cond, EffectDynamicCondition.class);
	}

	public static ICondition getItemDynamicCondition(String cond) {
		return getDynamicCondition(cond, ItemDynamicCondition.class);
	}

	private abstract static class BaseDynamicCondition implements ICondition { 
		private CompiledScript _cs;
		private String _funcName;
		private String _cond;
		protected BaseDynamicCondition(String cond) {
			_funcName =  String.format("checkDCond%d",(_index++));
			_cond = cond;
			
			String java = "function "+_funcName+"("+getFuncParams()+") {";
			if(!cond.contains("return"))
				java+=" return ("+cond+");";
			else 
				java+=cond;
			java+="}";
			try {
				_cs = L2ScriptEngineManager.getInstance().compile(_jsEngine, java);
				if(_cs!=null)
					_cs.eval();
			} catch(Exception e) {
				_log.error("Condition compilation failed!");
				System.out.println("============ JS Code ==============");
				System.out.println(_cond);
				System.out.println("============= ERROR ===============");
				e.printStackTrace();
			}
		
		}
		protected abstract String getFuncParams();
		@Override
		public synchronized boolean isValid(Object... params) {
			if(_cs==null)
				return false;
			Invocable invocable = (Invocable) _cs.getEngine();
			try {
				return (Boolean)invocable.invokeFunction(_funcName, params);
			} catch(Exception e) {
				_log.error("Error calculating `"+_cond+"`",e);
			}
			return false;
		}
	}
	private static class HTMLDynamicCondition extends BaseDynamicCondition {

		public HTMLDynamicCondition(String cond) {
			super(cond);
		}

		@Override
		protected String getFuncParams() {
			return "player,npc,item";
		}
	}
	private static class SkillDynamicCondition extends BaseDynamicCondition {
		public SkillDynamicCondition(String cond) {
			super(cond);
		}

		@Override
		protected String getFuncParams() {
			return "player,skill,target";
		}
	}
	private static class EffectDynamicCondition extends BaseDynamicCondition {
		public EffectDynamicCondition(String cond) {
			super(cond);
		}

		@Override
		protected String getFuncParams() {
			return "player,skill,effector";
		}
	}
	private static class ItemDynamicCondition extends BaseDynamicCondition {
		public ItemDynamicCondition(String cond) {
			super(cond);
		}

		@Override
		protected String getFuncParams() {
			return "player,item";
		}
	}
	
}
