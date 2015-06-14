package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.managers.Config;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

public class ConfigViewController {

	@FXML private VBox root;
	
	@FXML private ComboBox<String> startScreenSelector;
	
	private Runnable onClose;
	
	@FXML
	private void initialize() {
		populateScreenComboBox();
	}
	
	private void populateScreenComboBox() {
		ObservableList<String> items = startScreenSelector.getItems();
		
		items.add("Open Last View");
		items.add("Open New Entry View");
		items.add("Open Entry List View");
		
		startScreenSelector.setValue(Config.get().getPropertyAndSave("start_screen", "Open Last View"));
	}
	
	@FXML
	private void useDropbox() {
		
	}
	
	@FXML
	private void useCustomLocation() {
		
	}
	
	@FXML
	private void changeStartScreen() {
		Config.get().setProperty("start_screen", startScreenSelector.getValue());
	}
	
	public void setOnClose(Runnable onClose) {
		this.onClose = onClose;
	}
	
	@FXML
	private void close() {
		onClose.run();
	}
	
}
