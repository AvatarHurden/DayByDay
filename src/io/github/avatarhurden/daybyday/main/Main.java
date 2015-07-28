package io.github.avatarhurden.daybyday.main;

import io.github.avatarhurden.daybyday.components.DayOneTray;
import io.github.avatarhurden.daybyday.components.DropdownPane;
import io.github.avatarhurden.daybyday.controllers.MainWindowController;
import io.github.avatarhurden.daybyday.managers.Config;
import io.github.avatarhurden.daybyday.models.Journal;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.controlsfx.control.NotificationPane;

public class Main extends Application {

	private static final double version = 0.1;
	private static final String versionName = "0.1";
	private static final String changelogURL = "https://raw.githubusercontent.com/AvatarHurden/DaybyDay/master/changelog";

	private Stage primaryStage;
	MainWindowController controller;
	
	private DayOneTray trayIcon;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;

		setPosition(primaryStage);
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
		
		NotificationPane pane = new NotificationPane(loader.load());
		Scene scene = new Scene(pane);
		
		primaryStage.setTitle("Day by Day");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		primaryStage.getIcons().add(new Image(("images/Line.png")));
		
		if (!Journal.isInitiliazed()) {
			Pane node = showSplash(loader.getRoot());
			scene.setRoot(node);
		}
		
		new Thread(() -> {
			controller = loader.<MainWindowController>getController();
			Platform.runLater(() -> controller.loadState());
			
			Journal journal = new Journal(Config.get().getProperty("data_folder"));
			controller.setJournal(journal);
			Platform.runLater(() -> { 
				journal.loadAndWatch();
			});
			journal.setKeepEmptyEntry(true);

			
			primaryStage.setOnCloseRequest(event -> exit());
			
			startUpdater(pane);
		}).start();
		
//		trayIcon = new DayOneTray(primaryStage);
//		SystemTray.getSystemTray().add(trayIcon.getTrayIcon());
//		trayIcon.setExitAction(this::exit);
		
//		Platform.setImplicitExit(false);
	}
	
	private Pane showSplash(Pane parent) {
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SplashPage.fxml"));
		try {
			loader.load();
		} catch (Exception e) {
			return parent;
		}
		
		DropdownPane pane = new DropdownPane(parent, loader.getRoot());
		pane.setSpacing(25);
		new Thread(() -> {
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Platform.runLater(() -> pane.show(true));
		}).start();
		return pane;
	}
	
	private void startUpdater(NotificationPane pane) {
		new Thread(() -> {
			Updater up = new Updater(version, versionName, changelogURL, pane);
			up.start();
		}).start();
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
			controller.getJournal().close();
			controller.saveState();
			Config.save();
			Platform.exit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
