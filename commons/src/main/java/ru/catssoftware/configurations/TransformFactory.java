package ru.catssoftware.configurations;

import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;


import ru.catssoftware.configurations.transformers.BooleanTransformer;
import ru.catssoftware.configurations.transformers.ByteTransformer;
import ru.catssoftware.configurations.transformers.CharTransformer;
import ru.catssoftware.configurations.transformers.ClassTransformer;
import ru.catssoftware.configurations.transformers.DoubleTransformer;
import ru.catssoftware.configurations.transformers.EnumTransformer;
import ru.catssoftware.configurations.transformers.FileTransformer;
import ru.catssoftware.configurations.transformers.FloatTransformer;
import ru.catssoftware.configurations.transformers.InetSocketAddressTransformer;
import ru.catssoftware.configurations.transformers.IntegerTransformer;
import ru.catssoftware.configurations.transformers.LongTransformer;
import ru.catssoftware.configurations.transformers.ShortTransformer;
import ru.catssoftware.configurations.transformers.StringTransformer;

public class TransformFactory {
	private static Map<Class<?>, PropertyTransformer<?>> _transformers = new HashMap<Class<?>, PropertyTransformer<?>>();
	public static 	PropertyTransformer<?> getTransformer(Field f) 
	throws TransformationException
{
	Class<?> clazzToTransform = f.getType();
	// Just a hack, we can't set null to annotation value
		if(clazzToTransform == Boolean.class || clazzToTransform == Boolean.TYPE)
		{
			return BooleanTransformer.SHARED_INSTANCE;
		}
		else if(clazzToTransform == Byte.class || clazzToTransform == Byte.TYPE)
		{
			return ByteTransformer.SHARED_INSTANCE;
		}
		else if(clazzToTransform == Character.class || clazzToTransform == Character.TYPE)
		{
			return CharTransformer.SHARED_INSTANCE;
		}
		else if(clazzToTransform == Double.class || clazzToTransform == Double.TYPE)
		{
			return DoubleTransformer.SHARED_INSTANCE;
		}
		else if(clazzToTransform == Float.class || clazzToTransform == Float.TYPE)
		{
			return FloatTransformer.SHARED_INSTANCE;
		}
		else if(clazzToTransform == Integer.class || clazzToTransform == Integer.TYPE)
		{
			return IntegerTransformer.SHARED_INSTANCE;
		}
		else if(clazzToTransform == Long.class || clazzToTransform == Long.TYPE)
		{
			return LongTransformer.SHARED_INSTANCE;
		}
		else if(clazzToTransform == Short.class || clazzToTransform == Short.TYPE)
		{
			return ShortTransformer.SHARED_INSTANCE;
		}
		else if(clazzToTransform == String.class)
		{
			return StringTransformer.SHARED_INSTANCE;
		}
		else if(clazzToTransform.isEnum())
		{
			return EnumTransformer.SHARED_INSTANCE;
			// TODO: Implement
			// } else if (ClassUtils.isSubclass(clazzToTransform,
			// Collection.class)) {
			// return new CollectionTransformer();
			// } else if (clazzToTransform.isArray()) {
			// return new ArrayTransformer();
		}
		else if(clazzToTransform == File.class)
		{
			return FileTransformer.SHARED_INSTANCE;
		}
		else if(InetSocketAddress.class.isAssignableFrom(clazzToTransform))
		{
			return InetSocketAddressTransformer.SHARED_INSTANCE;
		}
		else if(clazzToTransform == Class.class)
		{
			return ClassTransformer.SHARED_INSTANCE;
		}
		else
		{
			
			if(_transformers.containsKey(clazzToTransform))
				return  _transformers.get(clazzToTransform);
			else
				for(Class<?> clazz : _transformers.keySet()) {
					if(clazz.isAssignableFrom(clazzToTransform)) {
						return _transformers.get(clazz);
					}
				}
			throw new TransformationException("No transformer registred for class "+clazzToTransform.getName());
		}
		
	}
	public static void registerTransformer(Class<?> clazz, PropertyTransformer<?> transformer) {
		_transformers.put(clazz, transformer);
	}

}
