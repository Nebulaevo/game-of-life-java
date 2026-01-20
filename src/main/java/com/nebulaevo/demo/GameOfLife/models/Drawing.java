package com.nebulaevo.demo.GameOfLife.models;

/**
 * Represents a drawable pattern that can be applied to the grid.
 * <p>
 * A {@code Drawing} consists of a textual template describing the shape
 * and an offset indicating the top-left position where the shape
 * should be applied.
 *
 * @param shape
 *        Array of strings representing the shape.
 *        Each string corresponds to a row; each character represents a cell.
 * @param offset
 *        Position of the top-left corner of the template in the grid.
 */
public record Drawing(String[] shape, Position offset) {}
