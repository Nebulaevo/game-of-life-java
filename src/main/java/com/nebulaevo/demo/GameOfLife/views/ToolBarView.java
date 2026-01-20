package com.nebulaevo.demo.GameOfLife.views;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.nebulaevo.demo.GameOfLife.models.Drawing;
import com.nebulaevo.demo.GameOfLife.models.Position;
import com.nebulaevo.demo.GameOfLife.models.ScheduledAction;
import com.nebulaevo.demo.GameOfLife.models.Shape;
import com.nebulaevo.demo.GameOfLife.models.SimulationStats;
import com.nebulaevo.demo.GameOfLife.models.Size;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;

/** UI element rendering buttons and simulation statistics */
public class ToolBarView extends FlowPane {
	
	private final static String STYLE =
		"-fx-pref-height: 40px; -fx-padding: 10px; ";
	
	Button playBtn;
	Button clearBtn;
	Button smallGrid;
	Button largeGrid;
	Button addShapeMaxBtn;
	Button addShapeGliderGunsBtn;
	Button addP80GunsBtn;
	
	Label fpsDisplay;
	Label cellsPerSecDisplay;
	Label cpuCoresDisplay;
	
	/**
	 * Creates and initialises a {@code ToolBarView} instance.
	 * <p>
	 * Including all the buttons and actions.
	 * 
	 * @param scheduleAction
	 * 		Callback adding an action to the scheduled actions
	 * 
	 * @param setCellSize
	 * 		Callback setting the size of a cell on the {@code GridView} canvas.
	 * 
	 * @param setCellMargin
	 * 		Callback setting the margin between cells on the {@code GridView} canvas.
	 * 
	 * @param scheduleDrawing
	 * 		Callback adding a drawing to the scheduled drawings
	 * 
	 * @param getGridCurrentSize
	 * 		Callback getting the current world grid size
	 */
	public ToolBarView(
			final Consumer<ScheduledAction> scheduleAction, 
			final Consumer<Integer> setCellSize,
			final Consumer<Integer> setCellMargin,
			final Consumer<Drawing> scheduleDrawing,
			final Supplier<Size> getGridCurrentSize
	) {
		super(Orientation.HORIZONTAL, 5, 5);
		this.setStyle(ToolBarView.STYLE);
		
		this.initControlBtns(scheduleAction, setCellSize, setCellMargin);
		this.initShapeInsertionBtns(scheduleDrawing, getGridCurrentSize);
		this.initStatsticDisplays();
		
		this.getChildren().addAll(
			this.sectionTitle("CONTROLS"),
			this.playBtn, this.clearBtn, this.smallGrid, this.largeGrid,
			this.separatorFactory(),
			this.sectionTitle("INSERT SHAPES"),
			this.addShapeMaxBtn, this.addShapeGliderGunsBtn, this.addP80GunsBtn,
			this.separatorFactory(),
			this.sectionTitle("STATS (" + Runtime.getRuntime().availableProcessors() + " CPU cores)"),
			this.fpsDisplay, new Label("Frames per seconds"),
			this.cellsPerSecDisplay, new Label("cells updated per seconds")
		);
	}
	
	/**
	 * Creates the instance's control buttons
	 * 
	 * @param scheduleAction
	 * 		Callback adding an action to the scheduled actions
	 * 
	 * @param setCellSize
	 * 		Callback setting the size of a cell on the {@code GridView} canvas.
	 * 
	 * @param setCellMargin
	 * 		Callback setting the margin between cells on the {@code GridView} canvas.
	 */
	private void initControlBtns(
			final Consumer<ScheduledAction> scheduleAction,
			final Consumer<Integer> setCellSize,
			final Consumer<Integer> setCellMargin
	) {
		this.playBtn = buttonFactory(
				"▶ Play",
				"-fx-pref-width: 80px;",
				e -> scheduleAction.accept(ScheduledAction.TOGGLE_PLAY)
		);
		
		this.clearBtn = buttonFactory("🗑 Clear", e -> scheduleAction.accept(ScheduledAction.CLEAR));
		this.smallGrid = buttonFactory("Small Grid", e -> {
			setCellSize.accept(3);
			setCellMargin.accept(1);
			scheduleAction.accept(ScheduledAction.RESIZE_GRID);
		});
		this.largeGrid = buttonFactory("Large Grid", e -> {
			setCellSize.accept(1);
			setCellMargin.accept(0);
			scheduleAction.accept(ScheduledAction.RESIZE_GRID);
		});
	}
	
	/**
	 * Creates the instance's shape insertion buttons
	 * 
	 * @param scheduleDrawing
	 * 		Callback adding a drawing to the scheduled drawings
	 * 
	 * @param getGridCurrentSize
	 * 		Callback getting the current world grid size
	 */
	private void initShapeInsertionBtns(
			final Consumer<Drawing> scheduleDrawing,
			final Supplier<Size> getGridCurrentSize
	) {
		this.addShapeMaxBtn = buttonFactory("MAX", e -> {
			// automatically position shape in the middle
			Size gridSize = getGridCurrentSize.get();
			Size shapeSize = Shape.sizeOf(Shape.MAX);
			
			Position pos = new Position(
				(gridSize.width() / 2) - (shapeSize.width() / 2),
				(gridSize.height() / 2) - (shapeSize.height() / 2)
 			);
			
			scheduleDrawing.accept(new Drawing(Shape.MAX, pos));
		});
		
		this.addShapeGliderGunsBtn = buttonFactory("Glider Guns", e -> {
			// automatically fits as many shapes as possible on the top
			Size gridSize = getGridCurrentSize.get();
			Size shapeSize = Shape.sizeOf(Shape.GLIDER_GUN);
			
			Position pos = new Position(0,0);
			while ( pos.x()+shapeSize.width() <= gridSize.width() ) {
				scheduleDrawing.accept(new Drawing(Shape.GLIDER_GUN, pos));
				
				pos = new Position(pos.x()+shapeSize.width()+10 ,0);
			}
		});
		
		this.addP80GunsBtn = buttonFactory("P80 Gliderless Guns", e -> {
			// automatically fits as many shapes as possible on the left
			Size gridSize = getGridCurrentSize.get();
			Size shapeSize = Shape.sizeOf(Shape.P_80_GLIDERLESS_HWSS_GUN);
			
			Position pos = new Position(0,0);
			while ( pos.y()+shapeSize.height() <= gridSize.height() ) {
				scheduleDrawing.accept(new Drawing(Shape.P_80_GLIDERLESS_HWSS_GUN, pos));
				
				pos = new Position(0, pos.y()+shapeSize.height()+10);
			}
		});
	}
	
	/** Creates the instance's statistics displays */
	private void initStatsticDisplays() {
		this.fpsDisplay = this.statisticsDisplayFactory(40);
		this.cellsPerSecDisplay = this.statisticsDisplayFactory(70);
	}
	
	/**
	 * Init display of dynamic values on certain buttons and labels.
	 * 
	 * @param playProperty
	 * 		boolean property indicating if the simulation should run
	 * 
	 * @param simulationStats
	 * 		{@code SimulationStats} instance keeping track of the simulation's statistics
	 */
	public void setupDataBindings(final BooleanProperty playProperty, final SimulationStats simulationStats) {
		this.playBtn.textProperty().bind(
		    Bindings.when(playProperty)
	            .then("⏸ Pause")
	            .otherwise("▶ Play")
		);
		
		this.fpsDisplay.textProperty().bind(simulationStats.avgComputedFpsProperty);
		this.cellsPerSecDisplay.textProperty().bind(simulationStats.avgComputedCellsPerSecProperty);
	}
	
	/**
	 * Returns a {@code Button} instance with the provided style
	 * 
	 * @param label
	 * 		Button text
	 * 
	 * @param style
	 * 		Button style
	 * 
	 * @param onClick
	 * 		Button click handler
	 */
	private Button buttonFactory(final String label, final String style, final EventHandler<ActionEvent> onClick) {
		Button btn = this.buttonFactory(label, onClick);
		btn.setStyle(style);
		
		return btn;
	}
	
	/**
	 * Returns a {@code Button} instance
	 * 
	 * @param label
	 * 		Button text
	 * 
	 * @param onClick
	 * 		Button click handler
	 */
	private Button buttonFactory(final String label, final EventHandler<ActionEvent> onClick) {
		Button btn = new Button(label);
		btn.setOnAction(onClick);
		
		return btn;
	}
	
	/** Returns a {@code Separator} instance with left and right padding */
	private Separator separatorFactory() {
		Separator sep = new Separator(Orientation.VERTICAL);
		sep.setStyle("-fx-padding: 0 7 0 7;");
		return sep;
	}
	
	/** 
	 * Returns a {@code Label} instance formatted as a section title
	 * 
	 * @param title
	 * 		The title text
	 */
	private Label sectionTitle(final String title) {
		Label label = new Label(title);
		label.setStyle("-fx-text-fill: grey;");
		return label;
	}
	
	/** 
	 * Returns a {@code Label} instance aimed at displaying a dynamic statistics value 
	 * 
	 * @param width
	 * 		The width of the label
	 * 		(to prevent size from changing evey time the value is updated)
	 */
	private Label statisticsDisplayFactory(final int width) {
		Label label = new Label(" - ");
		label.setAlignment(Pos.CENTER_RIGHT);
		label.setStyle(
			"-fx-font-weight: bold; "
			+ "-fx-pref-width: " + width + "px;"
		);
		return label;
	}
}
