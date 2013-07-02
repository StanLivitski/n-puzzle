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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import name.livitski.games.puzzle.android.model.Game;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.AdapterView;
import android.widget.TextView;

/**
 * User's interface to image selector page.
 */
public class ImageSelection extends Activity implements AdapterView.OnItemClickListener
{
 public ImageSelection()
 {
  super(R.layout.images);
 }

 @Override
 protected void onCreate(Bundle savedInstanceState)
 {
  super.onCreate(savedInstanceState);
  initUserImageSuffixes();

  GridView grid = (GridView)findViewById(R.id.image_selection_grid);
  grid.setOnItemClickListener(this);
  if (null == imageSource)
   imageSource = new ImageSource();
  imageSource.setContext(this);
  imageSource.init();
  updateContentView();
 }

 public void onItemClick(AdapterView<?> parent, View view, int position, long id)
 {
  final ImageSource.ImageWithConstraints imageInfo =
    (ImageSource.ImageWithConstraints)parent.getItemAtPosition(position);
  final Serializable imageId = imageInfo.getImageId();
  if (deletionMode)
  {
   deleteUserImage((String)imageId);
   deletionMode = false;
   updateContentView();
  }
  else
  {
   final Game.Level initialComplexity = imageInfo.getInitialComplexity();
   final Intent data = new Intent();
   data.putExtra(EXTRA_SELECTED_IMAGE_ID_KEY, imageId);
   if (null != initialComplexity)
    data.putExtra(EXTRA_SELECTED_IMAGE_INITIAL_LEVEL, initialComplexity);
   setResult(RESULT_OK, data);
   if (imageId instanceof String)
    markUserImageSelected((String)imageId);
   finish();
  }
 } 

 @Override
 public boolean onCreateOptionsMenu(Menu menu)
 {
  MenuInflater inflater = getMenuInflater();
  inflater.inflate(R.menu.pics_menu, menu);
  return true;
 }

 @Override
 public boolean onPrepareOptionsMenu(Menu menu)
 {
  return !deletionMode;
 }

 @Override
 public boolean onOptionsItemSelected(MenuItem item)
 {
  switch (item.getItemId())
  {
  case R.id.item_add_picture:
   selectPictureToAdd();
   break;
  case R.id.item_delete_picture:
   deletionMode = true;
   updateContentView();
   break;
  default:
   return super.onOptionsItemSelected(item);
  }
  return true;
 }

 @Override
 public void onBackPressed()
 {
  if (deletionMode)
  {
   deletionMode = false;
   updateContentView();
  }
  else
   super.onBackPressed();
 }

 @Override
 protected void onActivityResult(int requestCode, int resultCode, Intent data)
 {
  if (ACTIVITY_ADD_PICTURE == requestCode && RESULT_OK == resultCode)
  {
   final Uri imageURI = data.getData();
   if (null != imageAdder)
   {
    String msg = getResources().getString(R.string.image_load_error);
    Log.e(LOG_TAG, "Another image add operation is in progress");
    alert(msg);
   }
   final Handler handler = progress(getResources().getString(R.string.image_load_progress), null);
   final Display display = getWindowManager().getDefaultDisplay();
   int maxDisplaySize = display.getWidth();
   if (maxDisplaySize < display.getHeight())
    maxDisplaySize = display.getHeight();
   imageAdder = new ImageConverter(this, handler)
   {
    @Override
    public void run()
    {
     InputStream pictureData = null;
     OutputStream cacheOutput = null;
     String userImageId = null;
     try
     {
      BitmapFactory.Options bitmapInfo = prepareOptions();
      pictureData = openInputStream();
      synchronized(imageSource)
      {
       userImageId = allocateUserImage();
       cacheOutput = new FileOutputStream(ImageSource.imageFileForId(userImageId, ImageSelection.this));
       if (null != bitmapInfo)
       {
        // downsample and save
        final Bitmap bitmap = BitmapFactory.decodeStream(pictureData, null, bitmapInfo);
        bitmap.compress(CompressFormat.JPEG, 85, cacheOutput);
       }
       else
       {
        // copy image as-is
        byte buffer[] = new byte[16384];
        for (int read; (read = pictureData.read(buffer)) >= 0;)
 	cacheOutput.write(buffer, 0, read);
       }
       cacheOutput.close();
       cacheOutput = null;
      }
     }
     catch (Throwable failure)
     {
      setStatus(failure);
     }
     finally
     {
      if (null != pictureData)
       try { pictureData.close(); } catch (Exception ignored) {}
      if (null != cacheOutput)
       try { cacheOutput.close(); } catch (Exception ignored) {}
      if (null != userImageId)
       try { imageSource.onImageUpdate(userImageId); }
       catch (Exception failure)
       {
	Log.e(LOG_TAG, "Thumbnail update failed for " + userImageId, failure);
	if (null == getStatus())
	 setStatus(failure);
       }
      getParentHandler().sendEmptyMessage(0);
     }
    }
   };
   imageAdder.setImageURI(imageURI);
   imageAdder.setMaxFrameDimension(maxDisplaySize);
   submitBackgroundTask(imageAdder);
  }
 }

 @Override
 protected void onPause()
 {
  super.onPause();
  saveSettings();
 }

 @Override
 protected void onCompletion()
 {
  if (null != imageAdder)
  {
   if (null != imageAdder.getStatus())
   {
    String msg = getResources().getString(R.string.image_load_error);
    Log.e(LOG_TAG, msg, imageAdder.getStatus());
    alert(msg);
   }
   imageSource.notifyDataSetChanged();
   imageAdder = null;
  }
 }

 protected void updateContentView()
 {
  final View promptView = findViewById(R.id.image_selection_prompt_comment);
  if (deletionMode)
  {
   ((TextView)findViewById(R.id.image_selection_prompt)).setText(R.string.image_prompt_delete);
   final BaseAdapter source = imageSource.userImageSource();
   if (0 == source.getCount())
   {
    ((TextView)promptView).setText(R.string.image_nothing_to_delete_comment);
    promptView.setVisibility(View.VISIBLE);
   }
   else
    promptView.setVisibility(View.INVISIBLE);
   final GridView grid = (GridView)findViewById(R.id.image_selection_grid);
   grid.setAdapter(source);
  }
  else
  {
   ((TextView)findViewById(R.id.image_selection_prompt)).setText(R.string.image_prompt);
   ((TextView)promptView).setText(R.string.image_prompt_comment);
   promptView.setVisibility(View.VISIBLE);
   GridView grid = (GridView)findViewById(R.id.image_selection_grid);
   grid.setAdapter(imageSource);
  }
 }

 protected void selectPictureToAdd()
 {
  Intent request = new Intent();
  request.setAction(Intent.ACTION_PICK);
  request.setType(MIME_TYPE_IMAGE);
  try
  {
   startActivityForResult(request, ACTIVITY_ADD_PICTURE);
  }
  catch (ActivityNotFoundException noManager)
  {
   String msg = getResources().getString(R.string.no_gallery_error);
   Log.e(LOG_TAG, msg, noManager);
   alert(msg);
  }
 }

 protected void deleteUserImage(final String id)
 {
  final File file = ImageSource.imageFileForId(id, this);
  final String suffix = extractUserImageSuffix(id);
  synchronized (imageSource)
  {
   if (file.exists() && !file.delete())
   {
    String msg = getResources().getString(R.string.image_delete_error);
    Log.e(LOG_TAG, msg + ", file: " + file);
    alert(msg);
   }
   else
   {
    synchronized (userImageSuffixesLRU)
    {
     userImageSuffixesLRU.remove(suffix);
     userImageSuffixesLRU.add(0, suffix);
    }
   }
   imageSource.updateUserImages();
  }
 }

 /**
  * Allocates a cache file name for the new user image and
  * returns it. Removes the last recently used file from cache
  * if necessary.
  */
 protected String allocateUserImage()
 {
  final String index;
  synchronized (userImageSuffixesLRU)
  {
   index = userImageSuffixesLRU.remove(0);
   userImageSuffixesLRU.add(index);
  }
  final String name = ImageSource.IMAGE_FILE_PREFIX + index + ImageSource.IMAGE_FILE_SUFFIX;
  final File file = ImageSource.imageFileForId(name, this);
  if (file.exists())
   file.delete();
  return name;
 }

 /**
  * Moves the suffix of recently used file to the end of
  * the least recently used queue.
  */
 protected void markUserImageSelected(final String name)
 {
  final String index = extractUserImageSuffix(name);
  synchronized (userImageSuffixesLRU)
  {
   userImageSuffixesLRU.remove(index);
   userImageSuffixesLRU.add(index);
  }
 }

 protected String extractUserImageSuffix(final String name)
 {
  if (!name.startsWith(ImageSource.IMAGE_FILE_PREFIX)
    || !name.endsWith(ImageSource.IMAGE_FILE_SUFFIX))
    throw new IllegalArgumentException("File name \"" + name
      + "\" is not valid for the internal copy of a user's image.");
  final String index = name.substring(ImageSource.IMAGE_FILE_PREFIX.length(),
    name.length() - ImageSource.IMAGE_FILE_SUFFIX.length());
  return index;
 }

 protected void saveSettings()
 {
  SharedPreferences.Editor settings = getPreferences(MODE_PRIVATE).edit();
  saveUserImageSuffixes(settings);
  settings.commit();
 }

 protected void initUserImageSuffixes()
 {
  SharedPreferences preferences = getPreferences(MODE_PRIVATE);
  String lruString = preferences.getString(SETTING_USER_IMAGE_LRU, null);
  Set<String> unusedSuffixes = new HashSet<String>(ImageSource.MAX_USER_IMAGE_COUNT, 1f);
  for (int i = 0; ImageSource.MAX_USER_IMAGE_COUNT > i; i++)
   unusedSuffixes.add(Integer.toString(i));
  userImageSuffixesLRU = new LinkedList<String>();
  synchronized (userImageSuffixesLRU)
  {
   if (null != lruString)
   {
    for (
      StringTokenizer suffixesLRU = new StringTokenizer(lruString, DELMINTER_LRU_STRING);
      suffixesLRU.hasMoreTokens();
    )
    {
     String suffix = suffixesLRU.nextToken();
     userImageSuffixesLRU.add(suffix);
     unusedSuffixes.remove(suffix);
    }
   }
   userImageSuffixesLRU.addAll(0, unusedSuffixes);
  }
 }

 protected void saveUserImageSuffixes(SharedPreferences.Editor settings)
 {
  synchronized (userImageSuffixesLRU)
  {
   if (userImageSuffixesLRU.isEmpty())
    settings.remove(SETTING_USER_IMAGE_LRU);
   else
   {
    StringBuilder lruString = new StringBuilder();
    for (Iterator<String> it = userImageSuffixesLRU.iterator();;)
    {
     String index = it.next();
     lruString.append(index);
     if (it.hasNext())
      lruString.append(DELMINTER_LRU_STRING);
     else
      break;
    }
    settings.putString(SETTING_USER_IMAGE_LRU, lruString.toString());
   }
  }
 }

 protected static final String EXTRA_SELECTED_IMAGE_ID_KEY = "selected_image_id";
 protected static final String EXTRA_SELECTED_IMAGE_INITIAL_LEVEL = "selected_image_initial_level";

 protected static final String SETTING_USER_IMAGE_LRU = "user_image_lru";
 protected static final String DELMINTER_LRU_STRING = ",";
 protected static final String MIME_TYPE_IMAGE = "image/*";

 protected static final int ACTIVITY_ADD_PICTURE = 0;
 protected static final int DIALOG_ALERT = Integer.MAX_VALUE - 10;
 private static final String LOG_TAG = "ImageSelection";

 private boolean deletionMode;
 private ImageConverter imageAdder;
 /** Note: synchronized access, synchronize AFTER {@link #imageSource}. */
 private List<String> userImageSuffixesLRU;
 private static ImageSource imageSource;
}
