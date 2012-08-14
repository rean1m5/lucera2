package ru.catssoftware.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * Аннотация чтения из файла .properties<br>
 * Использование:<br>
 * <i>@Property(key="zz",defaultValue="100")<br>
 * private static int zz;</i><br>
 * Будет  прочитано zz=xxx, если в файле нет  такого параметра, то zz будет 10
 * @author Nick
 *
 */
public @interface Property {
	/**
	 * Имя параметра свойства
	 * @return
	 */
	public String key();
	/**
	 * Значение по умолчанию
	 * @return
	 */
	public String defaultValue() default "";
}
