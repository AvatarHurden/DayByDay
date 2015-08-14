package io.github.avatarhurden.daybyday.controllers;

import io.github.avatarhurden.daybyday.managers.Config;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

import javax.swing.JFileChooser;

public class SplashPageController {

	@FXML
	private AnchorPane root;
	@FXML
	private TextField folderField;
	
	private Runnable action;
	
	@FXML
	private void initialize() {
		folderField.setText(new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath());
	}
	
	@FXML
	private void changeFolder() {
		File current = new File(folderField.getText());
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setInitialDirectory(current);
		
		File chosen = chooser.showDialog(root.getScene().getWindow());
	
		if (chosen != null)
			folderField.setText(chosen.getAbsolutePath());
	}
	
	@FXML
	private void accept() {
		Config.get().setProperty("data_folder", new File(folderField.getText(), "Journal").getAbsolutePath());
		action.run();
	}
	
	public void setOnClose(Runnable action) {
		this.action = action;
	}
	
}
