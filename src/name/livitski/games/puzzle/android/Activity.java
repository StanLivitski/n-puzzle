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

import java.util.Formatter;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

/**
 * Provides common methods to the application's activities.
 */
public abstract class Activity extends android.app.Activity
{
 public Activity()
 {
 }

 public Activity(int contentViewResource)
 {
  this.contentView = contentViewResource;
 }

 public Activity(View contentView)
 {
  this.contentView = contentView;
 }

 @Override
 public void setContentView(int layoutResID)
 {
  if (null == contentView || !contentView.equals(layoutResID))
   forceSetContentView(layoutResID);
 }

 @Override
 public void setContentView(View view, LayoutParams params)
 {
  ViewGroup frame = (ViewGroup)findViewById(R.id.viewgroup_app_frame);
  if (frame.getChildAt(frameViewCount) != null)
   frame.removeViewAt(frameViewCount);
  addContentToFrame(frame, view, params);
 }

 @Override
 public void setContentView(View view)
 {
  if (view != contentView)
   forceSetContentView(view);
 }

 @Override
 public void setTitle(CharSequence title)
 {
  Formatter formatter = new Formatter()
  	.format(title.toString(), getResources().getString(R.string.app_name));
  super.setTitle(formatter.toString());
 }

 @Override
 public void setTitle(int titleId)
 {
  Resources resources = getResources();
  super.setTitle(resources.getString(titleId, resources.getString(R.string.app_name)));
 }

 /**
  * Displays a progress dialog with a message.
  * @param message message to display
  * @param max end of the progress range
  * or <code>null</code> to show an indeterminate dialog
  * @return the handler to receive progress messages
  * from the worker thread
  * @throws IllegalStateException if there already is an 
  * active progress dialog
  */
 public Handler progress(String message, Integer max)
 {
  if (null != progressHandler)
   throw new IllegalStateException("Cannot show progress of operation \""
     + message + "\" while another operation is in progress.");
  final ProgressDialog dialog = getProgressDialog();
  dialog.setMessage(message);
  if (null == max)
  {
   dialog.setIndeterminate(true);
   dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
  }
  else
  {
   dialog.setIndeterminate(false);
   dialog.setMax(max);
   dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
  }
  progressHandler = new ProgressHandler();
  showDialog(PROGRESS_DIALOG);
  return progressHandler;
 }

 /**
  * Displays dialog with a message and single confirmation button.
  */
 public void alert(String message)
 {
  alert = message;
  showDialog(ALERT_DIALOG);
 }

 /**
  * Displays dialog with a message and yes/no buttons.
  */
 public void choice(String message)
 {
  choice = message;
  showDialog(CHOICE_DIALOG);
 }

 @Override
 public void finish()
 {
  finished = true;
  super.finish();
 }

 @Override
 public Application getApplicationContext()
 {
  return (Application)super.getApplication();
 }

 /**
  * @return last {@link #onPause() paused} subclass, or
  * <code>null</code> if no subclass has yet been paused
  * @throws ClassNotFoundException if saved activity class is
  * no longer available or saved value was corrupted  
  * @throws ClassCastException if saved activity class is
  * not a subclass of {@link Activity}
  * @throws RuntimeException if there is an error reading
  * preferences file
  */
 @SuppressWarnings("unchecked")
 public Class<? extends Activity> getLastPausedActivity()
 	throws ClassNotFoundException
 {
  String className = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
  	.getString(LAST_ACTIVITY, null);
  if (null == className)
   return null;
  Class<?> clazz = Class.forName(className);
  if (!Activity.class.isAssignableFrom(clazz))
   throw new ClassCastException(className);
  return (Class<? extends Activity>)clazz;
 }

 protected Future<?> submitBackgroundTask(Runnable task)
 {
  return getApplicationContext().submitBackgroundTask(task);
 }

 protected <T> Future<T> submitBackgroundTask(Callable<T> task)
 {
  return getApplicationContext().submitBackgroundTask(task);
 }
 
 /**
  * Override to react to a standard dialog's cancellation. 
  */
 protected void onDialogCancel(int dialogId)
 {
 }

 /**
  * Override to react to user's input in a standard dialog. 
  */
 protected void onDialogResponse(int dialogId, int response)
 {
 }

 /**
  * Override to react to the completion of background task that
  * used a {@link #progress(String, Integer) progress dialog}. 
  */
 protected void onCompletion()
 {
 }

 @Override
 protected void onCreate(Bundle savedInstanceState)
 {
  super.onCreate(savedInstanceState);
  Application app = getApplicationContext();
  app.onActivityCreate(this);
  finished = false;
  super.setContentView(R.layout.frame);
  ViewGroup frame = (ViewGroup)findViewById(R.id.viewgroup_app_frame);
  frameViewCount = frame.getChildCount();
  if (contentView instanceof Integer)
   forceSetContentView((Integer)contentView);
  else if (contentView instanceof View)
   forceSetContentView((View)contentView);
  findViewById(R.id.text_app_footer_author).setOnClickListener(LISTENER_CLICK_AUTHOR);
  findViewById(R.id.text_app_footer_license).setOnClickListener(LISTENER_CLICK_LICENSE);
 }

 @Override
 protected void onDestroy()
 {
  super.onDestroy();
  Application app = getApplicationContext();
  app.onActivityDestroy(this);
 }

 @Override
 protected void onPause()
 {
  super.onPause();
  try
  {
   Editor settings = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit();
   if (finished)
    settings.remove(LAST_ACTIVITY);
   else
    settings.putString(LAST_ACTIVITY, this.getClass().getName());
   settings.commit();
  }
  catch (Exception e)
  {
   Log.w(LOG_TAG, "Could not update the " + LAST_ACTIVITY
     + " preference in file " + SHARED_PREFS, e);
  }
 }

 @Override
 protected Dialog onCreateDialog(final int id)
 {
  DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
   public void onClick(DialogInterface dialog, int which)
   {
    dismissDialog(id);
    onDialogResponse(id, which);
   }
  };

  DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
   public void onCancel(DialogInterface dialog)
   {
    dismissDialog(id);
    onDialogCancel(id);
   }
  };

  if (id == PROGRESS_DIALOG)
  {
   final ProgressDialog dialog = getProgressDialog();
   dialog.setOnCancelListener(onCancelListener);
   return dialog;
  }

  Builder builder = new AlertDialog.Builder(this)
   .setTitle(R.string.app_name)
   .setMessage("")
   .setOnCancelListener(onCancelListener);
  switch (id)
  {
  case ALERT_DIALOG:
   builder.setNeutralButton(R.string.ok_button, onClickListener);
   break;
  case CHOICE_DIALOG:
   builder
    .setPositiveButton(R.string.yes, onClickListener)
    .setNegativeButton(R.string.no, onClickListener);
   break;
  default:
   return super.onCreateDialog(id);
  }
  return builder.create();
 }

 protected ProgressDialog getProgressDialog()
 {
  if (null == progressDialog)
  {
   progressDialog = new ProgressDialog(this)
   {
    @Override
    protected void onStop()
    {
     super.onStop();
     progressHandler = null;
    }
   };
   progressDialog.setTitle(R.string.app_name);
   progressDialog.setMessage("");
  }
  return progressDialog;
 }

 @Override
 protected void onPrepareDialog(int id, Dialog dialog)
 {
  switch (id)
  {
  case ALERT_DIALOG:
   ((AlertDialog)dialog).setMessage(alert);
   break;
  case CHOICE_DIALOG:
   ((AlertDialog)dialog).setMessage(choice);
   break;
  default:
   super.onPrepareDialog(id, dialog);
  }
 }

 protected static final int ALERT_DIALOG = Integer.MAX_VALUE - 10;
 protected static final int CHOICE_DIALOG = Integer.MAX_VALUE - 9;
 protected static final int PROGRESS_DIALOG = Integer.MAX_VALUE - 8;

 protected final OnClickListener LISTENER_CLICK_AUTHOR =
   new OnClickListener()
 {
  @Override
  public void onClick(View v)
  {
   Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_AUTHOR));
   startActivity(i);
   return;
  }
 };

 protected final OnClickListener LISTENER_CLICK_LICENSE =
   new OnClickListener()
 {
  @Override
  public void onClick(View v)
  {
   Intent i = new Intent(Activity.this, LicensePages.class);
   startActivity(i);
   return;
  }
 };

 protected class ProgressHandler extends Handler
 {
  @Override
  public void handleMessage(Message msg)
  {
   if (progressDialog.isIndeterminate()
     || progressDialog.getMax() <= msg.arg1)
   {
    onCompletion();
    dismissDialog(PROGRESS_DIALOG);
   }
   else 
   {
    if (msg.arg2 > 0)
     progressDialog.setSecondaryProgress(msg.arg2);
    if (msg.arg1 >= 0)
     progressDialog.setProgress(msg.arg1);
   }
  }
 }

 private void forceSetContentView(int layoutResID)
 {
  ViewGroup frame = (ViewGroup)findViewById(R.id.viewgroup_app_frame);
  if (frame.getChildAt(frameViewCount) != null)
   frame.removeViewAt(frameViewCount);
  View view = View.inflate(this, layoutResID, null);
  addContentToFrame(frame, view, null);
 }

 private void forceSetContentView(View view)
 {
  ViewGroup frame = (ViewGroup)findViewById(R.id.viewgroup_app_frame);
  if (frame.getChildAt(frameViewCount) != null)
   frame.removeViewAt(frameViewCount);
  if (null != view)
   addContentToFrame(frame, view, null);
 }

 private void addContentToFrame(ViewGroup frame, View view, LayoutParams params)
 {
  RelativeLayout.LayoutParams frameParams;
  if (null == params)
   frameParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
  else if (params instanceof RelativeLayout.LayoutParams)
   frameParams = (RelativeLayout.LayoutParams)params;
  else
   frameParams = new RelativeLayout.LayoutParams(params);
  frameParams.addRule(RelativeLayout.BELOW, R.id.text_app_title);
  frameParams.addRule(RelativeLayout.ABOVE, R.id.text_app_footer);
  frame.addView(view, frameViewCount, frameParams);
 }

 private static final String LOG_TAG = "Activity";
 private static final String SHARED_PREFS = LOG_TAG + ".prefs";
 private static final String LAST_ACTIVITY = "LastActivity";
 private static final String URL_AUTHOR = "http://www.livitski.com"; 

 private ProgressDialog progressDialog;
 private ProgressHandler progressHandler;
 private String alert;
 private String choice;
 private Object contentView;
 private boolean finished;
 private int frameViewCount;
}
