package com.nebulaevo.demo.GameOfLife.models;

/** Defines the possible rendering strategy for the grid view */
public enum CanvasDrawMode {
	
	/** Only redraws positions which state changed 
	 * using a {@code RedrawPositions}  object */
	OPTIMISED,
	
	/** Redraws the whole grid */
	FULL,
}
