package com.nebulaevo.demo.GameOfLife.models;

import java.util.List;

/**
 * Result object returned by a thread in charge of 
 * computing a chunk of the grid.
 * 
 * @param xStartIdx
 * 		The index of the first column of the generated chunk
 * 
 * @param gridChunk
 * 		The generated grid chunk
 * 
 * @param redrawPos
 * 		Positions where cell state have changed in the grid chunk
 * 		(used to limit the amount of drawing on render)
 * 
 * @param livingCellss
 * 		Positions of living cells in the grid chunk
 * 		(used to register candidate cell positions that might 
 * 		change state on next update)
 */
public record ThreadResult(
	int xStartIdx,
	boolean[][] gridChunk,
	RedrawPositions redrawPos, 
	List<Position> livingCells
) {
	
}
