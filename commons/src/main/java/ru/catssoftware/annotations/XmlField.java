package ru.catssoftware.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)

public @interface XmlField {
	public String propertyName() default "value";
	public String nodeName() default "";
	public String set() default "";
}
