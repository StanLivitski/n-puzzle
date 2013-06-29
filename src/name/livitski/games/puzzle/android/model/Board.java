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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Represents an n-Puzzle gaming board.
 */
public class Board
{
 /**
  * Returns this board's {@link #Board(int) size}.
  */
 public int getSize()
 {
  return layout.length;
 }

 /**
  * Returns the number of tiles on this board, including
  * the blank tile.
  */
 public int getTileCount()
 {
  return tiles.length;
 }

 /**
  * Returns the number of tiles currently at their 
  * {@link Tile#isOnTarget() target places} on this board.
  */
 public int getScore()
 {
  int score = 0;
  for (Tile tile : tiles)
   if (tile.isOnTarget())
    score++;
  return score;
 }

 /**
  * Returns the tile at a specific location.
  * @throws IllegalStateException if the board is empty
  * @throws IllegalArgumentException if the location is invalid
  */
 public Tile getTileAt(int row, int col)
 {
  if (0 > row || layout.length <= row)
   throw new IllegalArgumentException("Invalid row index " + row);
  if (0 > col || layout.length <= col)
   throw new IllegalArgumentException("Invalid column index " + col);
  Tile tile = layout[row][col];
  if (null == tile)
   throw new IllegalStateException("Cannot point at a tile: the board is empty");
  return tile;
 }

 /**
  * Returns a string of tile numbers for all board positions.
  * The numbers are comma-separated. The sequence is left-to-right
  * for tiles in a row, then top to bottom for rows.
  * @return a string of tile numbers
  * @throws IllegalStateException if the board is empty
  */
 public String getTileLayout()
 {
  StringBuilder buffer = new StringBuilder(3 * getTileCount());
  final int size = getSize();
  for (int i = 0; i < size; i++)
   for (int j = 0; j < size; j++)
    buffer.append(',').append(getTileAt(i, j).getNumber());
  return 0 == buffer.length() ? "" : buffer.substring(1);
 }

 /**
  * Returns the move of a blank field that will swap it
  * with this tile according to the rules. Returns
  * <code>null</code> if this tile is not a neighbor
  * of the blank tile, or if it denotes the blank field.
  * @throws IllegalArgumentException if the tile
  * does not belong to this board's tile set
  */
 public Move permittedMoveFor(final Tile tile)
 {
  final int number = tile.getNumber();
  if (0 > number || tiles.length <= number || tile != tiles[number])
   throw new IllegalArgumentException(tile.toString());
  final int yoffset = tile.getRow() - blank.getRow();
  final int xoffset = tile.getColumn() - blank.getColumn();
  Move move = null;
  switch(xoffset)
  {
  case 0: // same column
   switch (yoffset)
   {
   case -1: // tile is above the blank
    move = Move.UP;
    break;
   case 1: // tile is below the blank
    move = Move.DOWN;
    break;
   }
   break;
  case 1:
  case -1:
   // horizontal moves are allowed within a single row
   if (0 == yoffset)
    move = 0 > xoffset ? Move.LEFT : Move.RIGHT;
   break;
  }
  return move;
 }

 /**
  * Moves a tile according to the rules.
  * @throws IllegalStateException if the board is empty
  * @throws IllegalArgumentException if requested move would push
  * the blank tile outside the board
  */
 public void move(Move direction)
 {
  int row = blank.getRow();
  int col = blank.getColumn();
  if (0 > row || 0 > col)
   throw new IllegalStateException("Cannot make a move: the board is empty");
  if (direction.isHorizontal())
  {
   col += direction.getAmount();
   if (0 > col || layout.length <= col)
    throw new IllegalArgumentException("Cannot make a " + direction + " to column " + col);
  }
  else if (direction.isVertical())
  {
   row += direction.getAmount();
   if (0 > row || layout.length <= row)
    throw new IllegalArgumentException("Cannot make a " + direction + " to row " + row);
  }
  else
   throw new UnsupportedOperationException("Unimplemented " + direction);
  Tile tile = layout[row][col];
  swapTiles(tile, blank);
  noitfyMoveListeners(blank, tile);
 }

 public void addMoveListener(MoveListener listener)
 {
  listeners.add(listener);
 }

 public void addTileOnTargetListener(final TileOnTargetListener listener)
 {
  for (Tile tile : tiles)
   tile.addOnTargetListener(listener);
 }

 public void forEachTile(TileHandler handler)
 {
  for (Tile tile : tiles)
   handler.processTile(tile);
 }

 /**
  * Places tiles on the board in the reverse order.
  */
 protected void placeTilesReverse()
 {
  int row, col;
  row = col = 0;
  final int size = getSize();
  for (int i = getTileCount(); 0 < i--;)
  {
   Tile tile = tiles[i];
   placeTile(tile, row, col);
   if (size <= ++col)
   {
    row++;
    col = 0;
    assert size > row;
   }
  }
  // For even board sizes, swap tiles 1 and 2
  if (0 == layout.length % 2)
   // We don't allow board size of 0, so if the size is even, there will always be tiles 1 and 2
   swapTiles(tiles[1], tiles[2]);
 }

 /**
  * Places tiles on the board in the solved puzzle order.
  */
 protected void placeTilesOnTarget()
 {
  forEachTile(new TileHandler() {
   public void processTile(Tile tile)
   {
    placeTile(tile, tile.getTargetRow(), tile.getTargetColumn());
   }
  });
 }

 /**
  * Places tiles on the board according to a
  * {@link #getTileLayout() layout string).
  */
 protected void placeTiles(String layout)
 {
  StringTokenizer numbers = new StringTokenizer(layout, ",");
  final int size = getSize();
  Set<Integer> placedTileNos = new HashSet<Integer>(getTileCount(), 1f); 
  for (int i = 0, j = 0; numbers.hasMoreTokens();)
  {
   int number = Integer.parseInt(numbers.nextToken().trim());
   if (!placedTileNos.add(number))
    throw new IllegalArgumentException("Tile " + number + " has already been placed");
   placeTile(tiles[number], i, j);
   if (size <= ++j)
   {
    j = 0;
    ++i;
   }
  }
  if (placedTileNos.size() < getTileCount())
   throw new IllegalArgumentException("Some tiles have not been placed, expected "
     + getTileCount() + " tile(s), placed " + placedTileNos.size());
 }

 /**
  * Places tiles on the board in a random order.
  */
 protected void placeTilesRandom()
 {
  // obtain a random arrangement of tiles
  Random random = new Random();
  // assign each tile a different random key
  SortedMap<Integer, Tile> randomOrder = new TreeMap<Integer, Tile>(); 
  for (int i = getTileCount(); 0 < i--;)
  {
   Integer key;
   do key = random.nextInt();
   while (randomOrder.containsKey(key));
   randomOrder.put(key, tiles[i]);
  }
  // place tiles on the board in order of their random keys
  final int size = getSize();
  Iterator<Tile> sequence = randomOrder.values().iterator();
  for (int i = 0; i < size; i++)
   for (int j = 0; j < size; j++)
    placeTile(sequence.next(), i, j);
  // check if this arrangement is solvable (Calabro, 2005)
  int distanceMod2 = (blank.getRow() + blank.getColumn()) % 2;
  if (computePermutationSign() != distanceMod2)
   // if not, swap two non-blank tiles
   swapTiles(tiles[1], tiles[2]);
 }

 /**
  * Determines whether current placement of tiles is even.
  * For the explanation of this algorithm, please refer to:
  * Chris Calabro, Solving the 15-Puzzle, June 14, 2005.
  * (retrieved Mar, 9 2011 from http://cseweb.ucsd.edu/~ccalabro/essays/15_puzzle.pdf)
  */
 protected int computePermutationSign()
 {
  final int tileCount = getTileCount();
  final int size = getSize();
  // construct a permutation in a 1-based array
  int[] permutation = new int[tileCount + 1];
  int i = 1;
  for (int row = 0; row < size; row++)
   for (int col = 0; col < size; col++)
   {
    int number = layout[row][col].getNumber();
    if (0 == number) number = tileCount;
    permutation[i++] = number;
   }
  // run Calabro's algorithm over the permutation and return its result
  int s = 0;
  for (i = 1; tileCount >= i;)
   if (i != permutation[i])
   {
    int temp = permutation[i];
    permutation[i] = permutation[temp];
    permutation[temp] = temp;
    s = 1 - s;
   }
   else
    i++;
  return s;
 }

 protected void noitfyMoveListeners(Tile from, Tile to)
 {
  for (MoveListener l : listeners)
   l.tileMoved(from, to);
 }

 protected void establishTarget(Tile tile)
 {
  int number = tile.getNumber();
  if (0 == number)
   number = getTileCount();
  number--;
  final int edgeSize = getSize();
  tile.target(number / edgeSize, number % edgeSize);
 }

 /**
  * Constructs an empty board of specified size and the associated
  * set of tiles. Before starting a game, one must place tiles on the
  * board.  
  * @param edgeSize the number of rows (and columns) on the board
  */
 protected Board(int edgeSize)
 {
  if (0 >= edgeSize || Integer.MAX_VALUE <= (long)edgeSize * edgeSize)
   throw new IllegalArgumentException("edgeSize = " + edgeSize);
  final int tileCount = edgeSize * edgeSize;
  this.layout = new Tile[edgeSize][edgeSize];
  this.tiles = new Tile[tileCount];
  for (int i = 0; tileCount > i; i++)
   establishTarget(tiles[i] = new Tile(i));
  this.blank = tiles[0];
 }

 protected interface TileHandler
 {
  void processTile(Tile tile);
 }

 private void swapTiles(Tile tile1, Tile tile2)
 {
  int row1 = tile1.getRow();
  int col1 = tile1.getColumn();
  placeTile(tile1, tile2.getRow(), tile2.getColumn());
  placeTile(tile2, row1, col1);
 }

 private void placeTile(Tile tile, int row, int col)
 {
  tile.place(row, col);
  layout[row][col] = tile;
 }

 private final Tile[][] layout;
 private final Tile[] tiles;
 private final Tile blank;
 private final List<MoveListener> listeners = new ArrayList<MoveListener>();
}
