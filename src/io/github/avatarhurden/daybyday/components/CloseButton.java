package io.github.avatarhurden.daybyday.components;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class CloseButton extends StackPane {
	
	public CloseButton() {
		getStylesheets().setAll("style/closeButton.css");
	    getStyleClass().addAll("clear-button");
		Region clearButton = new Region();
        clearButton.getStyleClass().addAll("graphic");
        
        getChildren().setAll(clearButton);
	}

	public void setOnAction(Runnable action) {
		setOnMouseReleased(event -> action.run());
	}
	
}
