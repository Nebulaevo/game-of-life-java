package com.nebulaevo.demo.GameOfLife;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;
import javafx.scene.Scene;

import com.nebulaevo.demo.GameOfLife.models.GridModel;
import com.nebulaevo.demo.GameOfLife.views.MainView;

public class App extends Application {
	
	private final static String TITLE = "Conway's Game of Life";
	private final static int MIN_WIDTH = 240;
	private final static int MIN_HEIGHT = 240;
	
	@Override
	public void start(Stage primaryStage) {
		// Meta Data
		primaryStage.setTitle(App.TITLE);
		primaryStage.setMaximized(true);
		primaryStage.setMinWidth(MIN_WIDTH);
		primaryStage.setMinHeight(MIN_HEIGHT);
		
		// Main Scene
		MainView mainView = new MainView();
		primaryStage.setScene(new Scene(mainView));
		primaryStage.show();
		
		// Initialises main scene content next tick 
		// (allows to reduce the amount of resize operations 
		// even though window size can still be 0,0 on the second tick sometimes)
		Platform.runLater(mainView::init);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
