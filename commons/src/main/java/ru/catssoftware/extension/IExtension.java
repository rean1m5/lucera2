package ru.catssoftware.extension;

public interface IExtension {
	public static abstract class ExtensionInfo {
		abstract public String getName();
		abstract public int getVersion();
		abstract public boolean installRequired();
	};
	public boolean load();
	public void init();
	public boolean install(boolean upgrade) throws Exception;
	public ExtensionInfo getInfo();
}
