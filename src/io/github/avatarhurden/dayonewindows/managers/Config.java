package io.github.avatarhurden.dayonewindows.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javafx.util.Callback;

public class Config {

	private static final Config config = new Config(new File("config.txt"));
	
	private File configFile;
	private Properties prop;	
//	private Ini apiIni;
	
	private Config(File configFile){
//		try {
//			apiIni = new Ini(apiFile);
//		} catch (IOException e) {
//			throw new IOException("API File is incorrect");
//		}
		this.configFile = configFile;
		prop = new Properties();
		try {
			prop.load(new FileInputStream(configFile));
		} catch (IOException e) {
			try {
				setDefaults();
			} catch (IOException e1) {}
		}
	}
	
	public Config(Config source) {
		this.configFile = source.configFile;
		this.prop = source.prop;
	}

	public static Config get() {
		return config;
	}
	
	public static void save() {
		try {
			config.saveConfig();
		} catch (IOException e) {}
	}

	public void restore(Config source) {
		this.configFile = source.configFile;
		this.prop = source.prop;
	}
	
	private void setDefaults() throws FileNotFoundException, IOException {
		prop.setProperty("default_folder", new File("data").getAbsolutePath());
		prop.setProperty("todo_file", "todo.txt");
		prop.setProperty("done_file", "done.txt");
		prop.store(new FileOutputStream(configFile), "");
	}
	
	public void setProperty(String name, String value) {
		prop.setProperty(name, value);
	}

	public String getProperty(String name) {
		return prop.getProperty(name);
	}
	
	public String getProperty(String name, String defaultValue) {
		return prop.getProperty(name, defaultValue);
	}
	
	public String getPropertyAndSave(String name, String defaultValue) {
		if (prop.getProperty(name) == null)
			prop.setProperty(name, defaultValue);
		return prop.getProperty(name);
	}
	
	public <S> S getProperty(String name, Callback<String, S> converter) {
		try {
			return converter.call(prop.getProperty(name));
		} catch (Exception e) {
			return null;
		}
	}
	
	public <S> S getProperty(String name, Callback<String, S> converter, S defaultValue) {
		try {
			return converter.call(prop.getProperty(name));
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public <S> S getPropertyAndSave(String name, Callback<String, S> decoder, S defaultValue, Callback<S, String> encoder) {
		if (prop.getProperty(name) == null)
			prop.setProperty(name, encoder.call(defaultValue));
		return decoder.call(prop.getProperty(name));
	}
	
	public void setListProperty(String name, String... values) {
		prop.setProperty(name, String.join(",", values));
	}
	
	public void setListProperty(String name, List<String> values) {
		prop.setProperty(name, String.join(",", values));
	}
	
	public <S> void setListProperty(String name, List<S> values, Callback<S, String> encoder) {
		List<String> strings = new ArrayList<String>();
		for (S value : values)
			strings.add(encoder.call(value));
		setListProperty(name, strings);
	}
	
	public List<String> getListProperty(String name) {
		try {
			return Arrays.asList(prop.getProperty(name).split(","));
		} catch (Exception e) {
			return null;
		}
	}
	
	public List<String> getListProperty(String name, List<String> defaultValue) {
		try {
			return Arrays.asList(prop.getProperty(name).split(","));
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public List<String> getListPropertyAndSave(String name, List<String> defaultValue) {
		if (prop.getProperty(name) == null)
			setListProperty(name, defaultValue);
		return Arrays.asList(prop.getProperty(name).split(","));
	}
	
	public <S> List<S> getListProperty(String name, Callback<String, S> converter) {
		try {
			List<S> objects = new ArrayList<S>();
			for (String value : prop.getProperty(name).split(","))
				objects.add(converter.call(value));
			return objects;
		} catch (Exception e) {
			return null;
		}
	}
	
	public <S> List<S> getListProperty(String name, Callback<String, S> converter, List<S> defaultValue) {
		try {
			List<S> objects = new ArrayList<S>();
			for (String value : prop.getProperty(name).split(","))
				objects.add(converter.call(value));
			return objects;
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public <S> List<S> getListPropertyAndSave(String name, Callback<String, S> decoder, List<S> defaultValue, Callback<S, String> encoder) {
		if (prop.getProperty(name) == null) {
			List<String> strings = new ArrayList<String>();
			for (S obj : defaultValue)
				strings.add(encoder.call(obj));
			setListProperty(name, strings);
		}
		List<S> objects = new ArrayList<S>();
		for (String value : prop.getProperty(name).split(","))
			objects.add(decoder.call(value));
		return objects;
	}
	
	public void saveConfig() throws FileNotFoundException, IOException {
		prop.store(new FileOutputStream(configFile), "LifeOrganizer Properties");
	}
	
	//	public void setDataFolder(String path) {
	//		prop.setProperty("default_folder", new File(path).getAbsolutePath());	
	//	}
	//	
	//	public File getDataFolder() {
	//		return new File(prop.getProperty("default_folder"));
	//	}
	//	
	//	public void setFile(String name, String path) {
	//		name += "_file";
	//		if (!new File(path).isAbsolute())
	//			path = new File("$default$", path).getPath();
	//		prop.setProperty(name, path);
	//	}
	//	
	//	public File getFile(String name) {
	//		name += "_file";
	//		String path = prop.getProperty(name);
	//		if (path.startsWith("$default$"))
	//			path = path.replace("$default$", prop.getProperty("default_folder"));
	//			
	//		return new File(path);
	//	}

//	public Properties getAPIProperties(String provider) {
//		Properties p = new Properties();
//		
//		Iterator<Entry<String, String>> set = apiIni.get(provider).entrySet().iterator();
//		while (set.hasNext()) {
//			Entry<String, String> e = set.next();
//			if (!e.getKey().startsWith("user_"))
//				p.put(e.getKey(), e.getValue());
//		}
//		return p;
//	}
//	
//	public void setAPIUserProperty(String provider, String key, String value) {
//		key = "user_" + key;
//		apiIni.get(provider).put(key, value);
//		try {
//			apiIni.store();
//		} catch (IOException e) {
//			System.out.println("Unable to save");
//		}
//	}
//	
//	public Properties getAPIUserProperties(String provider) {
//		Properties p = new Properties();
//		
//		Iterator<Entry<String, String>> set = apiIni.get(provider).entrySet().iterator();
//		while (set.hasNext()) {
//			Entry<String, String> e = set.next();
//			if (e.getKey().startsWith("user_"))
//				p.put(e.getKey().replace("user_", ""), e.getValue());
//		}
//		return p;
//	}
}
