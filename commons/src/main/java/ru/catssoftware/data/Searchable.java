package ru.catssoftware.data;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


public abstract class Searchable<T>  implements Iterable<T>,ISearchable<T> {
	private static Logger _log = Logger.getLogger("ERROR");
	private class SearchCriteria {
		Field _f;
		Method _m;
		Pattern _value;
		public SearchCriteria(String cond, T elem) {
			if(elem == null)
				return;
			String []v = cond.split("=");
			_value = Pattern.compile(v[1].trim());
			v[0] = v[0].trim();
			Class<?> c = elem.getClass();
			while(c!=null) {
				for(Field f :c.getClass().getDeclaredFields())
					if(f.getName().equalsIgnoreCase(v[0]) && f.isAccessible()) {
						_f = f;
						return;
					}
				for(Method m :c.getDeclaredMethods() ) {
					if(m.getName().equalsIgnoreCase("get"+v[0]) ) {
						_m = m;
						return;
					}
				}
				c = c.getSuperclass();
			}
		}
		public boolean isOk() { return _m!=null || _f!=null; }
		public boolean find(T element) {
			Object val = null;
			if(_f!=null) try {
				val = _f.get(element);
			} catch(Exception e) {
				return false;
			}
			else try {
				val = _m.invoke(element);
			} catch(Exception e) {
				return false;
			}
			if(val==null)
				return false;
			Matcher m = _value.matcher(val.toString());
			return m.find();
		}
		@Override
		public String toString() {
			String result = "";
			if(_f!=null)
				result+=_f.getName();
			if(_m!=null)
				result+=_m.getName()+"()";
			result+="="+_value.pattern();
			return result;
		}
	}

	/**
	 * Поиск по коллекции.<br>
	 * @param criteria - критерий поиска.
	 * @return
	 */
	public Searchable<T> search(String criteria) {
		SearchableList<T> result = new SearchableList<T>();
		try {
			T el = iterator().next();
			if(el==null)
				return result;
			List<SearchCriteria> _serach = new ArrayList<Searchable<T>.SearchCriteria>();
			for(String s : criteria.split(";")) {
				SearchCriteria c = new SearchCriteria(s,el);
				if(c.isOk()) 
					_serach.add(c);
				else 
					_log.warn("Criteria `"+s+"` not applyed for "+el.getClass().getName());
			}
			if(_serach.isEmpty())
				return result;
			for(T element : this) {
				for(SearchCriteria c : _serach) {
					if(c.find(element)) {
						result.add(element);
						break;
					}
				}
			}
		} catch(Exception e) {
			_log.error("Error finding `"+criteria+"` in "+getClass().getName(),e);
		}
		return result;
	}

}
