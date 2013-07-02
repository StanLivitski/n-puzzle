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
package name.livitski.games.puzzle.android;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import name.livitski.games.puzzle.android.R;
import name.livitski.games.puzzle.android.model.Game;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

/**
 * Provides a list of images from <code>/res/drawable</code>
 * with specifically {@link #IMAGE_FILE_PREFIX prefixed} names.
 */
public class ImageSource extends BaseAdapter
{
 @Override
 public synchronized int getCount()
 {
  return images.length;
 }

 @Override
 public synchronized ImageWithConstraints getItem(int index)
 {
  return images[index];
 }

 @Override
 public long getItemId(int index)
 {
  return index;
 }

 /**
  * You must call {@link #setContext(Context)} first to set the context
  * for displaying images.
  */
 @Override
 public synchronized View getView(final int position, View convertView, ViewGroup parent)
 {
  View view = images[position].getView();
  return view;
 }

 public BaseAdapter userImageSource()
 {
  if (null == userImageSource)
   userImageSource = new BaseAdapter()
   {
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
     synchronized (ImageSource.this)
     {
      if (position >= getCount())
       throw new IndexOutOfBoundsException("User image # " + position + " > " + getCount());
      return ImageSource.this.getView(position, convertView, parent);
     }
    }
    
    @Override
    public long getItemId(int position)
    {
     synchronized (ImageSource.this)
     {
      if (position >= getCount())
       throw new IndexOutOfBoundsException("User image # " + position + " > " + getCount());
      return ImageSource.this.getItemId(position);
     }
    }
    
    @Override
    public Object getItem(int position)
    {
     synchronized (ImageSource.this)
     {
      if (position >= getCount())
       throw new IndexOutOfBoundsException("User image # " + position + " > " + getCount());
      return ImageSource.this.getItem(position);
     }
    }
    
    @Override
    public int getCount()
    {
     synchronized (ImageSource.this)
     { return userImageIndexes.size(); }
    }
   };
  return userImageSource;
 }

 public static File imageFileForId(String id, Context context)
 {
  final File dir = context.getFilesDir();
  return new File(dir, id);
 }

 public synchronized String[] imageFileIds()
 {
  final File dir = context.getFilesDir();
  return dir.list(new FilenameFilter()
  {
   @Override
   public boolean accept(File dir, String filename)
   {
    return filename.startsWith(IMAGE_FILE_PREFIX);
   }
  });
 }

 public void setContext(Activity context)
 {
  this.context = context;
 }

 /**
  * Loads images into this container. You must call
  * {@link #setContext(Context)} first to set the context
  * for loading images.
  */
 public synchronized void init()
 {
  if (null != images)
   return;
  // store images sorted by their file name suffix
  SortedMap<String, Integer> builtinImageIds = new TreeMap<String, Integer>();
  for (Field field : R.drawable.class.getFields())
  try
  {
   if (Modifier.STATIC == (field.getModifiers() & Modifier.STATIC)
     && Integer.TYPE == field.getType() && field.getName().startsWith(IMAGE_FILE_PREFIX))
   {
    String suffix = field.getName().substring(IMAGE_FILE_PREFIX.length());
    int id = field.getInt(null);
    builtinImageIds.put(suffix, id);
   }
  }
  catch (IllegalAccessException skipNoAccess) {}
  images = new ImageEntry[builtinImageIds.size()];
  int i = 0;
  // add built-in images
  for (Map.Entry<String, Integer> entry : builtinImageIds.entrySet())
  {
   Integer id = entry.getValue();
   Integer difficulty = null;
   try
   {
    difficulty = Integer.valueOf(entry.getKey());
   }
   catch (NumberFormatException ignored) {}
   ImageEntry image;
   if (null != difficulty && 0 <= difficulty && 3 > difficulty)
    image = new ImageEntry(id, difficulty + 3);
   else
    image = new ImageEntry(id);
   images[i++] = image;
  }
  updateUserImages();
 }

 /**
  * Rescans the data directory picking up user image
  * changes. This method assumes that {@link #init()}
  * has been called before on the object.
  */
 public synchronized void updateUserImages()
 {
  int builtinImageCount = images.length;
  if (null != userImageIndexes)
   builtinImageCount -= userImageIndexes.size();
  userImageIndexes = new HashMap<String, Integer>(ImageSource.MAX_USER_IMAGE_COUNT, 1f);
  final String[] userImageIds = imageFileIds();
  ImageEntry[] oldImages = images;
  images = new ImageEntry[userImageIds.length + builtinImageCount];
  System.arraycopy(
    oldImages, oldImages.length - builtinImageCount, images, userImageIds.length, builtinImageCount);
  int i = 0;
  // add user's images first
  for (String id : userImageIds)
  {
   userImageIndexes.put(id, i);
   images[i++] = new ImageEntry(id);
  }
 }

 public synchronized void onImageUpdate(String userImageId)
 {
  Integer index = userImageIndexes.get(userImageId);
  if (null != index)
   images[index].updateImage();
  else
   updateUserImages();
 }

 private int getMaxThumbnailSize()
 {
  final LayoutParams size = getThumbnailSize();
  return size.width > size.height ? size.width : size.height;
 }

 private LayoutParams getThumbnailSize()
 {
  if (null == thumbnailSize)
  {
   DisplayMetrics metrics = new DisplayMetrics();
   context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
   thumbnailSize = new LayoutParams(
     (int)(metrics.density * GRID_CELL_WIDTH_DP),
     (int)(metrics.density * GRID_CELL_HEIGHT_DP)
   );
  }
  return thumbnailSize;
 }

 /**
  * Image file name prefix: <code>puzzle_</code>
  */
 public static final String IMAGE_FILE_PREFIX = "puzzle_";

 /**
  * Image file name suffix for user images in the cache.
  */
 public static final String IMAGE_FILE_SUFFIX = ".img";

 public static final int MAX_USER_IMAGE_COUNT = 3;

 protected static final int GRID_CELL_WIDTH_DP = 140;
 protected static final int GRID_CELL_HEIGHT_DP = 100;
 
 interface ImageWithConstraints
 {
  /** Resource id or file name of the image. */
  Serializable getImageId();
  /**
   * Initial game complexity associated with this image.
   * <code>null</code> if this image imposes no complexity
   * requirements on the game.
   */
  Game.Level getInitialComplexity();
 }

 private class ImageEntry implements ImageWithConstraints
 {
  @Override
  public Serializable getImageId()
  {
   return id;
  }

  @Override
  public Game.Level getInitialComplexity()
  {
   return initialComplexity;
  }

  public View getView()
  {
   if (null == frame)
    initView();
   return frame;
  }

  public void updateImage()
  {
   this.thumbnailCache = null;
   this.frame = null;
  }

  public ImageEntry(Serializable id)
  {
   this.id = id;
  }

  public ImageEntry(Serializable id, int initialBoardSize)
  {
   this.id = id;
   this.initialComplexity = Game.Level.forBoardSize(initialBoardSize);
  }
 
  private void initView()
  {
   frame = new FrameLayout(context);
   frame.setLayoutParams(getThumbnailSize());
   Bitmap thumbnail = getThumbnailCache();
   if (null != thumbnail)
    showThumbnail(thumbnail);
   else
   {
    showProgress();
    if (null == conversionJob)
     beginConversion();
   }
 }

  private void showProgress()
  {
   ProgressBar view = new ProgressBar(context);
   FrameLayout.LayoutParams params =
     new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
   params.gravity = Gravity.CENTER;
   view.setIndeterminate(true);
   frame.addView(view, params);
  }

  private void showThumbnail(Bitmap thumbnail)
  {
   ImageView view = new ImageView(context);
   FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(getThumbnailSize());
   params.gravity = Gravity.CENTER;
   view.setImageBitmap(thumbnail);
   frame.addView(view, params);
  }

  private void beginConversion()
  {
   final Serializable imageId = getImageId();
   final Handler thumbnailHandler = new Handler()
   {
    @Override
    public void handleMessage(Message result)
    {
     final ImageConverter converter;
     synchronized (ImageSource.this)
     {
      converter = conversionJob;
      conversionJob = null;
     }
     final Throwable status = converter.getStatus();
     if (null != status)
     {
      String msg = context.getResources().getString(R.string.image_load_error);
      Log.e(LOG_TAG, msg, status);
      context.alert(msg);
     }
     else
     {
      Bitmap thumbnail = converter.getBitmap();
      synchronized (ImageSource.this)
      {
       setThumbnailCache(thumbnail);
       if (null != frame)
       {
	frame.removeAllViews();
	showThumbnail(thumbnail);
       }
      }
     }
    }
   };
   final ImageConverter converter = new ImageConverter(context, thumbnailHandler);
   converter.setImageId(imageId);
   converter.setMaxFrameDimension(getMaxThumbnailSize());
   this.conversionJob = converter;
   context.submitBackgroundTask(converter);
  }
  
  private Bitmap getThumbnailCache()
  {
   // Allow the GC to dispose of thumbnails when low on memory, reload them when needed
   Bitmap thumbnail = null == this.thumbnailCache ? null : this.thumbnailCache.get();
   return thumbnail;
  }

  private void setThumbnailCache(Bitmap thumbnail)
  {
   this.thumbnailCache = new SoftReference<Bitmap>(thumbnail);
  }

  private Serializable id;
  private Game.Level initialComplexity;
  private Reference<Bitmap> thumbnailCache;
  private ImageConverter conversionJob;
  private FrameLayout frame;
 }

 private static final String LOG_TAG = "ImageSource";
 
 private Activity context;
 private BaseAdapter userImageSource;
 private ImageEntry[] images;
 private Map<String, Integer> userImageIndexes;
 private LayoutParams thumbnailSize;
}
