package ru.catssoftware.configurations.transformers;

import java.lang.reflect.Field;

import ru.catssoftware.configurations.PropertyTransformer;
import ru.catssoftware.configurations.TransformFactory;
import ru.catssoftware.configurations.TransformationException;
import ru.catssoftware.util.ArrayUtils;

public class IntArrayTransformer implements PropertyTransformer<int []> {
	static {
		TransformFactory.registerTransformer(int[].class, new IntArrayTransformer());
	}
	@Override
	public int[] transform(String value, Field field, Object... data)
			throws TransformationException {
		int [] result = {};
		for(String s : value.split(",")) try {
			result = ArrayUtils.add(result, Integer.valueOf(s.trim()));
		} catch(NumberFormatException nfe  ) {} 
			
		return result;
	}

}
