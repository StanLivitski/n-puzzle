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

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import name.livitski.games.puzzle.android.ImageSource;
import name.livitski.games.puzzle.android.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * A container for n-puzzle game data. 
 */
public class Game implements MoveListener, TileOnTargetListener
{
 /** Returns the difficulty level of this game. */
 public Level getDifficulty()
 {
  return difficulty;
 }

 /** Returns this game's board. */
 public Board getBoard()
 {
  return board;
 }

 /** Returns the number of moves made during this game. */
 public int getMoveCount()
 {
  return moveCount;
 }

 /** Tells whether the puzzle is solved. */
 public boolean isSolved()
 {
  return board.getTileCount() == score;
 }

 /** Tells whether the puzzle has an image assigned. */
 public boolean isImageSelected()
 {
  return null != selectedImageId;
 }

 public Serializable getSelectedImageId()
 {
  return selectedImageId;
 }

 /**
  * Assigns an image to the puzzle. Does not load
  * the new image. Call {@link #updateImage()} to load the
  * image, update aspect ratio and tiles.
  * @param id id of an image resource or name of the cached image file
  */
 public void setSelectedImage(Serializable id)
 {
  selectedImageId = id; 
  clearImages();
 }

 public void resetSelectedImage()
 {
  selectedImageId = null;
  clearImages();
 }

 public float getImageAspectRatio()
 {
  if (Float.isNaN(imageAspectRatio))
   throw new IllegalStateException("No image loaded");
  return imageAspectRatio;
 }

 public void setTileSize(int width, int height)
 {
  tileWidth = width;
  tileHeight = height;
 }

 public boolean isStarted()
 {
  return started;
 }

 public void preview()
 {
  if (started)
   throw new IllegalStateException("The game has been started, cannot show a preview");
  board.placeTilesOnTarget();
 }

 public void start()
 {
  this.moveCount = 0;
  board.placeTilesRandom();
  if (!started)
  {
   this.score = board.getScore();
   board.addMoveListener(this);
   board.addTileOnTargetListener(this);
  }
  this.started = true;
 }

 public void save(Editor settings)
 {
  settings.putString(DIFFICULTY_SETTING, getDifficulty().toString());
  if (!isImageSelected())
   settings.remove(IMAGE_ID_SETTING);
  else
  {
   Serializable id = getSelectedImageId();
   if (id instanceof Integer)
    settings.putInt(IMAGE_ID_SETTING, (Integer)id);
   else
    settings.putString(IMAGE_ID_SETTING, id.toString());
  }
  settings.putString(DIFFICULTY_SETTING, getDifficulty().toString());
  if (!isStarted())
  {
   settings.remove(MOVE_COUNT_SETTING);
   settings.remove(BOARD_STATE_SETTING);
  }
  else
  {
   settings.putInt(MOVE_COUNT_SETTING, getMoveCount());
   settings.putString(BOARD_STATE_SETTING, getBoard().getTileLayout());
  }
 }

 public void load(SharedPreferences state)
 {
  String layout = state.getString(BOARD_STATE_SETTING, null);
  if (null != layout)
  {
   moveCount = state.getInt(MOVE_COUNT_SETTING, 0);
   try
   {
    board.placeTiles(layout);
   }
   catch (RuntimeException badLayout)
   {
    Log.w(getClass().getName(), "Error loading game, parsing failed for \""
      + BOARD_STATE_SETTING + '"', badLayout);
    board.placeTilesRandom();
   }
   if (!started)
   {
    score = board.getScore();
    board.addMoveListener(this);
    board.addTileOnTargetListener(this);
   }
   this.started = true;
  }
 }

 public void updateImageSize(Context context)
 	throws ImageProcessingException
 {
  if (null == selectedImageId)
   throw new IllegalStateException("No image selected");
  try
  {
//   BitmapFactory.Options request = new BitmapFactory.Options();
//   request.inJustDecodeBounds = true;
//   BitmapFactory.decodeResource(context.getResources(), selectedImageId, request);
//   if (0 <= request.outWidth && 0 <= request.outHeight)
   Bitmap fullImage = loadFullImage(context);
   if (null != fullImage)
    imageAspectRatio = // (float)request.outWidth / request.outHeight;
   	(float)fullImage.getWidth() / fullImage.getHeight();
   else
    imageAspectRatio = Float.NaN;
  }
  catch (Resources.NotFoundException e)
  {
   throw new ImageProcessingException("Resource not found: 0x"
     + Integer.toHexString((Integer)selectedImageId), e);
  }
//  if (null == fullImage)
  if (Float.NaN == imageAspectRatio)
   throw new ImageProcessingException("Error loading image "
     + selectedImageId);
 }

 public void loadImage(final Context context)
	throws ImageProcessingException
 {
  if (null == selectedImageId)
   throw new IllegalStateException("No image selected");
  if (0 > tileHeight || 0 > tileWidth)
   throw new IllegalStateException("Target size is not set");
  // load and scale the image
  final int boardSize = board.getSize();
  try
  {
   Bitmap fullImage = loadFullImage(context);
   if (null == fullImage)
    throw new ImageProcessingException("Error loading image "
      + selectedImageId);
   scaledImage = Bitmap.createScaledBitmap(
     fullImage, tileWidth * boardSize, tileHeight * boardSize, false);
  }
  catch (Resources.NotFoundException e)
  {
   throw new ImageProcessingException("Resource not found: 0x"
     + Integer.toHexString((Integer)selectedImageId), e);
  }
  // slice the image
  board.forEachTile(new Board.TileHandler() {
   public void processTile(Tile tile)
   {
    Drawable image;
    if (0 == tile.getNumber())
     image = new ColorDrawable(context.getResources().getColor(R.color.blank_tile));
    else
    {
     Bitmap slice = Bitmap.createBitmap(
        scaledImage,
        tileWidth * tile.getTargetColumn(),
        tileHeight * tile.getTargetRow(),
        tileWidth,
        tileHeight
       );
     image = new BitmapDrawable(context.getResources(), slice);
    }
    tile.setDrawable(image);
   }
  });
 }

 public void tileMoved(Tile from, Tile to)
 {
  moveCount++;
 }

 public void tileOnTargetStateChanged(Tile tile, boolean onTarget)
 {
  score += onTarget ? 1 : -1;
 }

 /**
  * Creates a new game with specified difficulty level. 
  */
 public Game(Level difficulty)
 {
  this.difficulty = difficulty;
  board = new Board(difficulty.getBoardSize());
 }

 /**
  * Creates a new game with the default difficulty level. 
  */
 public Game()
 {
  this(DEFAULT_LEVEL);
 }

 /**
  * Difficulty level of a puzzle game.
  */
 public enum Level {
  EASY(3), MEDIUM(4), HARD(5);
  
  public int getBoardSize()
  {
   return boardSize;
  }

  public static Level forBoardSize(int boardSize)
  {
   if (3 > boardSize || 5 < boardSize)
    throw new IllegalArgumentException("Unsupported board size " + boardSize);
   return values()[boardSize - 3];
  }

  private Level(int boardSize)
  {
   this.boardSize = boardSize;
  }

  private int boardSize;
 }

 public static final Level DEFAULT_LEVEL = Level.MEDIUM; 
 public static final String IMAGE_ID_SETTING = "image_id";
 public static final String DIFFICULTY_SETTING = "difficulty";

 protected static final String BOARD_STATE_SETTING = "tiles";
 protected static final String MOVE_COUNT_SETTING = "move_count";

 private void clearImages()
 {
  fullImageCache = null;
  scaledImage = null;
  imageAspectRatio = Float.NaN;
  board.forEachTile(TILE_IMAGE_REMOVER);
 }

 private Bitmap loadFullImage(Context context)
 {
  Bitmap fullImage = null;
  if (null != fullImageCache)
   fullImage = fullImageCache.get();
  if (null == fullImage)
  {
   fullImage = selectedImageId instanceof Integer 
     ? BitmapFactory.decodeResource(context.getResources(), (Integer)selectedImageId)
     : BitmapFactory.decodeFile(
       ImageSource.imageFileForId((String)selectedImageId, context).getAbsolutePath());
   fullImageCache = null == fullImage ? null : new SoftReference<Bitmap>(fullImage);
  }
  return fullImage;
 }

 private Level difficulty;
 private Board board;
 private int moveCount, score;
 private Serializable selectedImageId;
 private boolean started;
 private int tileWidth = -1, tileHeight = -1;
 private float imageAspectRatio = Float.NaN;
 private Bitmap scaledImage;
 private Reference<Bitmap> fullImageCache;

 private static final Board.TileHandler TILE_IMAGE_REMOVER = new Board.TileHandler() {
  public void processTile(Tile tile)
  {
   tile.setDrawable(null);
  }
 };
}
