package io.github.avatarhurden.dayonewindows.managers;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class DirectoryWatcher {

	private long readDelay = 100;
	
	private List<Predicate<Path>> filters;
	private List<BiConsumer<Path, WatchEvent.Kind<?>>> actions;
	
	private List<Path> pathsToIgnore;
	private Map<Path, WatchEvent.Kind<?>> pathsToPerform;
	
	private boolean watch = true;
	private Path directory;
	
	WatchService watcher;
	
	public DirectoryWatcher(Path directory) {
		this.directory = directory;
		
		pathsToPerform = new HashMap<Path, WatchEvent.Kind<?>>();
		pathsToIgnore = new ArrayList<Path>();
		
		filters = new ArrayList<Predicate<Path>>();
		actions = new ArrayList<BiConsumer<Path, WatchEvent.Kind<?>>>();
	}
	
	public void addAction(BiConsumer<Path, WatchEvent.Kind<?>> action) {
		actions.add(action);
	}
	
	public void addFilter(Predicate<Path> filter) {
		filters.add(filter);
	}
	
	public void ignorePath(Path path) {
		pathsToIgnore.add(path);
	}
	
	public void watchPath(Path path) {
		pathsToIgnore.remove(path);
	}
	
	public void setReadDelay(long delay) {
		readDelay = delay;
	}
	
	public long getReadDelay() {
		return readDelay;
	}
	
	public void startWatching() throws IOException {
		watch = true;

		watcher = FileSystems.getDefault().newWatchService();
		directory.register(watcher, 
				StandardWatchEventKinds.ENTRY_CREATE, 
				StandardWatchEventKinds.ENTRY_DELETE, 
				StandardWatchEventKinds.ENTRY_MODIFY);
		
		while (watch) 
			waitForEvent(watcher);
	}
	
	public void stopWatching() {
		try {
			watcher.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		watch = false;
	}
	
	private void waitForEvent(WatchService watcher) {
		WatchKey key;
		try {
			key = watcher.take();
		} catch (InterruptedException e) {
			return; 
		}
	
		if (!watch) return;
		
		for (WatchEvent<?> event : key.pollEvents()) {
			WatchEvent.Kind<?> kind = event.kind();
        
			if (kind == StandardWatchEventKinds.OVERFLOW) continue;
        
			Path file = directory.resolve((Path) event.context());
        
			for (Predicate<Path> filter : filters)
				if (!filter.test(file))
					return;
			
			if (pathsToIgnore.contains(file))
				return;
			
			if (!pathsToPerform.containsKey(file))
				new Thread(() -> {
					try {
						Thread.sleep(readDelay);
					} catch (Exception e1) {}
					for (BiConsumer<Path, WatchEvent.Kind<?>> action : actions)
						action.accept(file, pathsToPerform.get(file));
					pathsToPerform.remove(file);
				}).start();

			pathsToPerform.put(file, kind);
		}

		key.reset();
	}
	
}