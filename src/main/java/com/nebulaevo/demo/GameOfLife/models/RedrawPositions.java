package com.nebulaevo.demo.GameOfLife.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Object listing the positions which state changed during the current update cycle
 * to limit the number of draw instructions during the canvas update.
 * 
 * @param dyingCellsPos
 * 		List of cells that are no longer alive (draw dead cell)
 * 
 * @param cellBirthsPos
 * 		List of cells that were born (draw alive cell)
 */
public record RedrawPositions(List<Position> dyingCellsPos, List<Position> cellBirthsPos) {
	
	/** Builds a fresh empty instance of {@code RedrawPositions} */
	public static RedrawPositions getFresh() {
		return new RedrawPositions(
			new ArrayList<Position>(), 
			new ArrayList<Position>()
		);
	}
	
	/**
	 * Injects all the positions registered from the 
	 * provided {@code RedrawPositions} instance
	 * 
	 * @param other
	 * 		{@code RedrawPositions} instance we want 
	 * 		to merge into the current instance
	 */
	public void inject(RedrawPositions other) {
		this.cellBirthsPos().addAll(other.cellBirthsPos());
		this.dyingCellsPos().addAll(other.dyingCellsPos());
	}
}
