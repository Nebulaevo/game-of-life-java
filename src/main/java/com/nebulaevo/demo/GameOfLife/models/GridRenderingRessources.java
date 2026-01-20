package com.nebulaevo.demo.GameOfLife.models;

/**
 * Resources provided to the render callback 
 * at the end of a {@code GridModel} update cycle.
 * 
 * @param grid
 * 		The world grid
 * 
 * @param size
 * 		Size of the current grid
 * 
 * @param redrawPositions
 * 		Provides positions that got modified by the update cycle
 * 		(to limit the number of drawing instructions during the canvas update)
 * 
 * @param updatedCellsCount
 * 		Number of candidate cells updated in the cycle
 * 		(for stats)
 */
public record GridRenderingRessources(
	boolean[][] grid,
	Size size,
	RedrawPositions redrawPositions,
	int updatedCellsCount
) {}
