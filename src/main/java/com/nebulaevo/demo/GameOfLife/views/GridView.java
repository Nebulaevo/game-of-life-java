package com.nebulaevo.demo.GameOfLife.views;

import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import com.nebulaevo.demo.GameOfLife.models.RedrawPositions;
import com.nebulaevo.demo.GameOfLife.models.Position;
import com.nebulaevo.demo.GameOfLife.models.Size;

/** Canvas based UI component rendering the world grid */
public class GridView extends Canvas {
	
	// STATIC ATTRIBUTES
	private final static int GRID_MARGIN = 5;
	private final static Color DEAD_COLOR = Color.LIGHTGREY;
	private final static Color ALIVE_COLOR = Color.CORNFLOWERBLUE;
	
	// INSTANCE ATTRIBUTES
	private int cellSize = 1;
	private int cellMargin = 0;
	private GraphicsContext ctx;
	
	public GridView() {
		super();
		this.ctx = this.getGraphicsContext2D();
	}
	
	/** 
	 * Sets up canvas automatic resizing
	 * <p>
	 * Binds the size of the canvas to the size of its parent
	 * and gives it the priority to grow within the parent's boundary 
	 * to automatically make it occupy all the available space in the
	 * window.
	 * 
	 * @param availableWidthProperty
	 * 		Property containing the available pixel width
	 * 		(width property of the parent)
	 * 
	 * @param availableHeightProperty
	 * 		Property containing the available pixel height
	 * 		(width property of the parent minus siblings height properties)
	 */
	public void setupSizeBindings(
		ObservableValue<Number> availableWidthProperty, 
		ObservableValue<Number> availableHeightProperty 
	) {
		// Give grid view priority on dynamic growth
		VBox.setVgrow(this, Priority.ALWAYS);
		
		// Binding canvas size to VBox Size
		this.widthProperty().bind( availableWidthProperty );
		this.heightProperty().bind( availableHeightProperty );
	}
	
	/**
	 * Returns the maximum grid size that can be drawn on the canvas
	 * <p>
	 * Number of columns and rows that can be drawn with the current 
	 * cell size and margin settings.
	 */
	public Size getMaxGridSize() {
		double width = this.getWidth() - (GRID_MARGIN*2) + this.cellMargin;
		double height = this.getHeight() - (GRID_MARGIN*2) + this.cellMargin;
		
		int cols = (int) width / (this.cellSize+this.cellMargin);
		int rows = (int) height / (this.cellSize+this.cellMargin);
		
		if (cols < 0) cols=0;
		if (rows < 0) rows=0;
		
		return new Size(cols, rows);
	}
	
	/** 
	 * Draws the given world grid on the canvas
	 * <p>
	 * Clears the whole canvas and draws the entire grid
	 * 
	 * @param grid
	 * 		Current world grid state
	 * 
	 * @param gridSize
	 * 		Current world grid size
	 */
	public void drawGrid(final boolean[][] grid, final Size gridSize) {
		Size uiSize = this.getMaxGridSize();
 		
		this.clear();
		for (int x=0; x<uiSize.width(); x++) {
			for (int y=0; y<uiSize.height(); y++) {
				Position pos = new Position(x, y);
				
				if (pos.isInBounds(gridSize) && grid[x][y]) {
					this.ctx.setFill(ALIVE_COLOR);
				} else {
					this.ctx.setFill(DEAD_COLOR);
				}
				
				drawCell(pos);
			}
		}
	}
	
	/** 
	 * Modifies the canvas to fit the new world grid state
	 * <p>
	 * Draws only the cells which state changed
	 * 
	 * @param grid
	 * 		Current world grid state
	 * 
	 * @param gridSize
	 * 		Current world grid size
	 * 
	 * @param redrawPos
	 * 		positions which state changed during the last update cycle
	 */
	public void drawGrid( 
			final boolean[][] grid, 
			final Size gridSize, 
			final RedrawPositions redrawPos
	) {
		Size uiSize = this.getMaxGridSize();
		
		this.ctx.setFill(DEAD_COLOR);
		for (Position pos: redrawPos.dyingCellsPos()) {
			if (pos.isInBounds(uiSize)) drawCell(pos);
		}
		
		this.ctx.setFill(ALIVE_COLOR);
		for (Position pos: redrawPos.cellBirthsPos()) {
			if (pos.isInBounds(uiSize)) drawCell(pos);
		}
	}
	
	/**
	 * Sets the desired cell size for the world grid rendering
	 * <p>
	 * This will affect the maximum drawable grid size
	 * 
	 * @param size
	 * 		Value in pixels used both as height and width of the cells
	 */
	public void setCellSize(final int size) {
		this.cellSize = size;
	}
	
	/**
	 * Sets the desired cell margin size for the world grid rendering
	 * <p>
	 * This will affect the maximum drawable grid size
	 * 
	 * @param margin
	 * 		Distance in pixels between cells in the rendered grid
	 */
	public void setCellMargin(final int margin) {
		this.cellMargin = margin;
	}
	
	/** Clears any rendered content from the canvas */
	private void clear() {
		this.ctx.clearRect(0, 0, this.getWidth(), this.getHeight());
	}
	
	/**
	 * Draws a cell on the canvas at the provided grid location
	 * <p>
	 * Grid position is converted to canvas pixel coordinates using
	 * the current cell size and margin settings
	 * 
	 * @param pos
	 * 		The column-row based position of the cell in the world grid
	 */
	private void drawCell(final Position pos) {
		int x = GRID_MARGIN + (this.cellSize+this.cellMargin) * pos.x();
		int y = GRID_MARGIN + (this.cellSize+this.cellMargin) * pos.y();
		this.ctx.fillRect(x, y, this.cellSize, this.cellSize);
	}
}

