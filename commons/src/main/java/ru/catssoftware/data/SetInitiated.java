package ru.catssoftware.data;

import java.lang.reflect.Field;

import ru.catssoftware.configurations.PropertyTransformer;
import ru.catssoftware.configurations.TransformFactory;
import ru.catssoftware.util.StatsSet;
/**
 * 
 * @author Nick
 * Базовый класс, принимающий в качестве параметра конструктора StatsSet<br>
 * Автоматически проставляет все поля значением из него<br>
 * <b>Пример</b><br>
 * class my  extends SetInitiated {<br>
 *   private int _val;<br>
 *   private int val1;<br>
 *  }<br>
 *   StatsSet s = new StatsSet();<br>
 *   s.set("val",10);<br>
 *   s.set("val1",20);<br>
 *   my m = new my(s);<br>
 *   Будут проинициализированы поля _val (10) и val1 (20) 
 */
public class SetInitiated {
	protected void load(StatsSet set) {
		Class<?> cl = getClass();
		while(cl!=SetInitiated.class) {
			for(Field f : cl.getDeclaredFields()) {
				String name = f.getName();
				if(name.startsWith("_"))
					name = name.substring(1);
				if(set.hasValueFor(name)) {
					 boolean access  = f.isAccessible();
					 f.setAccessible(true);
					 PropertyTransformer<?> t = TransformFactory.getTransformer(f);
					 if(t!=null) try {
						 String val = set.getString(name);
						 if(val.startsWith("#"))
							 val = getTableVal(val); 
						 f.set(this,t.transform(val, f));
					 } catch(Exception e) {}
					 f.setAccessible(access);
					 
				}
			}
			cl = cl.getSuperclass();
		}
	}
	protected String getTableVal(String val) {
		return val;
	}
}
