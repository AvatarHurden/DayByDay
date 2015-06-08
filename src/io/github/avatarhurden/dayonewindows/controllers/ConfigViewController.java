package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.managers.Config;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class ConfigViewController {

	@FXML private ComboBox<String> startScreenSelector;
	
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
	
}
