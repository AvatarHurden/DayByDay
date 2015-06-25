package io.github.avatarhurden.dayonewindows.main;

import io.github.avatarhurden.dayonewindows.controllers.DayOneTray;
import io.github.avatarhurden.dayonewindows.controllers.MainWindowController;
import io.github.avatarhurden.dayonewindows.managers.Config;
import io.github.avatarhurden.dayonewindows.managers.Journal;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.controlsfx.control.NotificationPane;

public class Main extends Application {

	private Stage primaryStage;
	MainWindowController controller;
	
	private DayOneTray trayIcon;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		
		Journal entryManager = new Journal(Config.get().getProperty("data_folder"));
		entryManager.loadAndWatch();

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
		
		NotificationPane pane = new NotificationPane(loader.load());
		Scene scene = new Scene(pane);
		
		primaryStage.setTitle("LifeOrganizer");
		primaryStage.setScene(scene);
		primaryStage.show();

		setPosition(primaryStage);
		
		controller = loader.<MainWindowController>getController();
		controller.setJournal(entryManager);
		
		primaryStage.setOnCloseRequest(event -> exit());
		
//		trayIcon = new DayOneTray(primaryStage);
//		SystemTray.getSystemTray().add(trayIcon.getTrayIcon());
//		trayIcon.setExitAction(this::exit);
		
//		Platform.setImplicitExit(false);
	}
	
	private void savePosition(Window window) {
		List<Double> pos = new ArrayList<Double>();
		pos.add(window.getX());
		pos.add(window.getY());
		pos.add(window.getWidth());
		pos.add(window.getHeight());
		Config.get().setListProperty("window_position", pos, d -> d.toString());
	}
	
	private void setPosition(Window window) {
		List<Double> pos = Config.get().getListProperty("window_position", s -> Double.valueOf(s), null);
		if (pos == null)
			return;
		
		Rectangle2D screen = Screen.getPrimary().getVisualBounds();

		Rectangle2D windowRect = new Rectangle2D(pos.get(0), pos.get(1), pos.get(2), pos.get(3));
		
		window.setWidth(pos.get(2));
		window.setHeight(pos.get(3));
		
		if (!screen.intersects(windowRect))
			window.centerOnScreen();
		else {
			window.setX(pos.get(0));
			window.setY(pos.get(1));
		}
	}
	
	public void exit() {
		try {
			savePosition(primaryStage);
//			SystemTray.getSystemTray().remove(trayIcon.getTrayIcon());
			Config.save();
			controller.getJournal().close();
			Platform.exit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
