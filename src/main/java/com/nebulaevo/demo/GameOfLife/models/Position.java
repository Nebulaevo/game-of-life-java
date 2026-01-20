package com.nebulaevo.demo.GameOfLife.models;

/** Represents a 2D position
 * 
 * @param x
 * 		column index
 * 
 * @param y
 * 		row index
 */
public record Position(int x, int y) {

	public boolean isInBounds(final Size gridSize) {
		return this.x >= 0 && this.x < gridSize.width()
			&& this.y >= 0 && this.y < gridSize.height();
	}
}
