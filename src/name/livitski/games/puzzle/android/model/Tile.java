/**
 * Copyright © 2011,2013 Konstantin Livitski
 * 
 * This file is part of n-Puzzle application. n-Puzzle application is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * n-Puzzle application contains adaptations of artwork covered by the Creative
 * Commons Attribution-ShareAlike 3.0 Unported license. Please refer to the
 * NOTICE.md file at the root of this distribution or repository for licensing
 * terms that apply to that artwork.
 * 
 * n-Puzzle application is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * n-Puzzle application; if not, see the LICENSE/gpl.txt file of this distribution
 * or visit <http://www.gnu.org/licenses>.
 */
package name.livitski.games.puzzle.android.model;

import java.util.LinkedList;
import java.util.List;

import android.graphics.drawable.Drawable;

/**
 * Represents a tile for n-Puzzle game.
 */
public class Tile
{
 /** Returns the number on this tile, or <code>0</code> for a blank tile. */
 public int getNumber()
 {
  return number;
 }

 /**
  * Returns the image for this tile.
  */
 public Drawable getDrawable()
 {
  return drawable;
 }

 /** Returns this tile's row on the board, or <code>-1</code> if the tile is not on the board. */
 public int getRow()
 {
  return row;
 }

 /** Returns this tile's column on the board, or <code>-1</code> if the tile is not on the board. */
 public int getColumn()
 {
  return column;
 }

 public int getTargetRow()
 {
  return targetRow;
 }

 public int getTargetColumn()
 {
  return targetColumn;
 }

 /**
  * Tells whether the tile is currently at its target position.
  */
 public boolean isOnTarget()
 {
  return 0 <= targetRow && targetRow == row
  	&& 0 <= targetColumn && targetColumn == column;
 }

 public void addOnTargetListener(TileOnTargetListener listener)
 {
  listeners.add(listener);
 }

 /** Describes this tile for debugging purposes. */
 @Override
 public String toString()
 {
  return "Tile " + number;
 }

 protected void setDrawable(Drawable drawable)
 {
  this.drawable = drawable;
 }

 protected void place(int row, int column)
 {
  final boolean onTargetBefore = isOnTarget();
  this.row = row;
  this.column = column;
  final boolean onTargetNow = isOnTarget();
  if (onTargetBefore != onTargetNow)
   onTargetStateChanged(onTargetNow);
 }

 protected void target(int row, int column)
 {
  final boolean onTargetBefore = isOnTarget();
  this.targetRow = row;
  this.targetColumn = column;
  final boolean onTargetNow = isOnTarget();
  if (onTargetBefore != onTargetNow)
   onTargetStateChanged(onTargetNow);
 }

 protected final void onTargetStateChanged(boolean stateNow)
 {
  for (TileOnTargetListener listener : listeners)
   listener.tileOnTargetStateChanged(this, stateNow);
 }

 protected Tile(int number)
 {
  this.number = number;
 }

 private int number;
 private int row = -1;
 private int column = -1;
 private int targetRow = -1;
 private int targetColumn = -1;
 private Drawable drawable;
 private final List<TileOnTargetListener> listeners = new LinkedList<TileOnTargetListener>();
}
