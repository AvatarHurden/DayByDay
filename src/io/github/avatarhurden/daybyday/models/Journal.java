package io.github.avatarhurden.daybyday.models;

import io.github.avatarhurden.daybyday.managers.Config;
import io.github.avatarhurden.daybyday.managers.DirectoryWatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.util.Callback;

public class Journal {
	
	private Path entryFolder, imageFolder;
	
	private ObservableMap<String, JournalEntry> entryMap;
	private ObservableList<Entry> entryList;
	
	private ObservableList<Tag> tagsList;
	
	private DirectoryWatcher watcher;
	private DirectoryWatcher imageWatcher;
	
	private BooleanProperty keepEmptyEntry;
	private JournalEntry emptyEntry;
	private ChangeListener<Boolean> emptyChange;
	
	public static boolean isInitiliazed() {
		return Config.get().getProperty("data_folder") != null;
	}
	
	public Journal(String folder) {
		Path rootFolder = Paths.get(folder);
		entryFolder = rootFolder.resolve("entries");
		imageFolder = rootFolder.resolve("photos");
		
		if (!entryFolder.toFile().exists())
			entryFolder.toFile().mkdirs();
		if (!imageFolder.toFile().exists())
			imageFolder.toFile().mkdirs();
			
		// List updates whenever the creation date of a entry is modified
		Callback<Entry,Observable[]> callback = entry -> new Observable[]{
				entry.creationDateProperty()
		};
		
		entryList = FXCollections.observableArrayList(callback);
		entryList.addListener((ListChangeListener.Change<? extends Entry> event) -> {
			while (event.next()) {
				if (event.wasRemoved())
					for (Entry entry : event.getRemoved())
						entryMap.remove(entry.getUUID());
				if (event.wasAdded())
					for (Entry entry : event.getAddedSubList())
						if (entry instanceof JournalEntry)
							entryMap.put(entry.getUUID(), (JournalEntry) entry);
			}
		});
		
		entryMap = FXCollections.observableHashMap();
		
		tagsList = FXCollections.observableArrayList();
		
		keepEmptyEntry = new SimpleBooleanProperty(false);
		
		keepEmptyEntry.addListener((obs, oldValue, newValue) -> {
			if (newValue) {
				addEmptyEntry();
			} else
				emptyEntry = null;
		});
		
		emptyChange = (obs, oldValue, newValue) -> {
			if (!newValue && keepEmptyEntry.get()) 
				addEmptyEntry();
		};
	}
	
	public JournalEntry getEntry(String id) {
		return entryMap.get(id);
	}
	
	public void loadAndWatch() {
		try {
			entryList.clear();
			tagsList.clear();
			entryMap.clear();
			
			readFolder();
			
			loadImages();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		listenToFolder();
	}
	
	public void changeFolder(String folder) {
		Path rootFolder = Paths.get(folder);
		entryFolder = rootFolder.resolve("entries");
		imageFolder = rootFolder.resolve("photos");
		
		close();
		loadAndWatch();
	}
	
	public void close() {
		watcher.stopWatching();
		imageWatcher.stopWatching();
	}
	
	public File getEntryFolder() {
		return entryFolder.toFile();
	}
	
	public File getImageFolder() {
		return imageFolder.toFile();
	}
	
	public void loadImages() throws IOException {
		for (Entry entry : entryList) {
			if (!(entry instanceof JournalEntry))
					continue;
			String id = entry.getUUID() + ".jpg";
		  	File file = new File(imageFolder.toFile(), id);
		  	if (file.exists())
		  		((JournalEntry) entry).setImageFile(file);
		}
	}
	
	public ObservableList<Entry> getEntries() {
		return entryList;
	}
	
	public void addTag(String tag, JournalEntry entry) {
		for (Tag t : tagsList) {
			if (t.getName().equals(tag)) {
				t.getEntries().add(entry);
				return;
			}
		}
		Tag t = new Tag(tag);
		t.getEntries().add(entry);
		tagsList.add(t);
	}
	
	public void removeTag(String tag, JournalEntry entry) {
		for (Tag t : tagsList)
			if (t.getName().equals(tag)) {
				t.getEntries().remove(entry);
				if (t.getEntries().isEmpty())
					tagsList.remove(t);
				return;
			}
	}
	
	public Tag getTag(String name) {
		for (Tag t : tagsList)
			if (t.getName().equals(name)) 
				return t;
		return null;
	}
	
	public ObservableList<Tag> getTags() {
		return tagsList;
	}
	
	public void ignoreForAction(String uuid, Runnable action) {
		try {
			ignoreEntry(uuid);
		} catch (Exception e) {}
		
		action.run();
		
		new Thread(() -> {
			try {
				Thread.sleep(100);
				removeIgnore(uuid);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).run();
	}
	
	public void ignoreEntry(String uuid) {
		JournalEntry entry = getEntry(uuid);
		watcher.ignorePath(entry.getFile().toPath());
		if (entry.getImageFile() != null)
			imageWatcher.ignorePath(entry.getImageFile().toPath());
	}
	
	public void removeIgnore(String uuid) {
		JournalEntry entry = getEntry(uuid);
		watcher.watchPath(entry.getFile().toPath());
		if (entry.getImageFile() != null)
			imageWatcher.watchPath(entry.getImageFile().toPath());
	}
	
	public boolean getKeepEmptyEntry() {
		return keepEmptyEntry.get();
	}
	
	public void setKeepEmptyEntry(boolean value) {
		keepEmptyEntry.setValue(value);
	}
	
	public BooleanProperty keepEmptyEntryProperty() {
		return keepEmptyEntry;
	}

	public JournalEntry addEntry() {
		JournalEntry t = JournalEntry.createNewEntry(this);
		entryList.add(t);
		return t;
	}
	
	private void addEmptyEntry() {
		if (emptyEntry != null)
			emptyEntry.emptyProperty().removeListener(emptyChange);
		emptyEntry = addEntry();
		
		emptyEntry.emptyProperty().addListener(emptyChange);
	}

	public JournalEntry getEmptyEntry() {
		return emptyEntry;
	}
	
	public void deleteEntry(JournalEntry entry) {
		entryList.remove(entry);
	}
	
	private void readFolder() throws Exception {
		DirectoryStream<Path> stream = Files.newDirectoryStream(entryFolder, 
				path -> Pattern.matches("^[0-9abcdefABCDEF]{32}\\.doentry", path.getFileName().toString()));
		for (Path file: stream) {
			JournalEntry entry = JournalEntry.loadFromFile(this, file.toFile());
			entryList.add(entry);
		   	for (String tag : entry.getTags()) 
		   		addTag(tag, entry);
		}
	}
	
	private void listenToFolder() {
		watcher = new DirectoryWatcher(entryFolder);
		watcher.addFilter(path -> Pattern.matches("^[0-9abcdefABCDEF]{32}\\.doentry", path.getFileName().toString()));
		
		watcher.addAction((path, kind) -> readFile(path, kind));
		
		imageWatcher = new DirectoryWatcher(imageFolder);
		imageWatcher.addFilter(path -> Pattern.matches("^[0-9abcdefABCDEF]{32}\\.jpg", path.getFileName().toString()));
		
		imageWatcher.addAction((path, kind) -> readImageFile(path, kind));
		
		new Thread(() -> {
			try {
				watcher.startWatching();
			} catch (Exception e) {}	
		}).start();
		
		new Thread(() -> {
			try {
				imageWatcher.startWatching();
			} catch (Exception e) {}	
		}).start();
	}
	
	private void readImageFile(Path path,  WatchEvent.Kind<?> kind) {
		String id = path.getFileName().toString().replace(".jpg", "");
		JournalEntry entry = getEntry(id);
		
		Platform.runLater(() -> entry.setImageFile(path.toFile()));
	}
	
	private void readFile(Path path, WatchEvent.Kind<?> kind) {
		String id = path.getFileName().toString().replace(".doentry", "");
		JournalEntry entry = getEntry(id);
		System.out.println("detected file " + path);
		
		Platform.runLater(() -> {
			if (kind == StandardWatchEventKinds.ENTRY_CREATE)
				try {
					entryList.add(JournalEntry.loadFromFile(this, path.toFile()));
				} catch (Exception e) {	
					e.printStackTrace();
				}
			else if (kind == StandardWatchEventKinds.ENTRY_DELETE)
				entryList.remove(entry);
			else if (kind == StandardWatchEventKinds.ENTRY_MODIFY)
				try { // It can happen that a file is created and modified before being read, so test if it is in the map
					if (entry == null)
						entryList.add(JournalEntry.loadFromFile(this, path.toFile()));
					else
						entry.readFile();
				} catch (Exception e) {
					e.printStackTrace();
				}
		});
	}
	
}
