package ru.catssoftware.configurations.transformers;

import java.lang.reflect.Field;
import java.math.BigInteger;

import ru.catssoftware.configurations.PropertyTransformer;
import ru.catssoftware.configurations.TransformFactory;
import ru.catssoftware.configurations.TransformationException;

public class BigIntegerTransformer implements PropertyTransformer<BigInteger>{
	static {
		TransformFactory.registerTransformer(BigInteger.class, new BigIntegerTransformer());
	}
	@Override
	public BigInteger transform(String value, Field field, Object... data)
			throws TransformationException {
		return new BigInteger(value);
	}
	

}
