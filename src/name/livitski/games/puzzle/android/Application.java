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

import java.util.IdentityHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This project's application context. Allocates and maintains
 * resources used throughout the project's activities.
 */
public class Application extends android.app.Application
{
 public Future<?> submitBackgroundTask(Runnable task)
 {
  return getBackgroundThreads().submit(task);
 }

 public <T> Future<T> submitBackgroundTask(Callable<T> task)
 {
  return getBackgroundThreads().submit(task);
 }
 
 public void onActivityCreate(Activity activity)
 {
  activities.put(activity, null);
 }

 public void onActivityDestroy(Activity activity)
 {
  activities.remove(activity);
  if (activities.isEmpty())
   onTerminate();
 }

 /**
  * This method is called when all application's
  * activities have been {@link #onActivityDestroy(Activity) destroyed}. 
  */
 @Override
 public void onTerminate()
 {
  if (null != backgroundThreads)
  {
   backgroundThreads.shutdown();
   backgroundThreads = null;
  }
  super.onTerminate();
 }

 protected ExecutorService getBackgroundThreads()
 {
  if (null == backgroundThreads)
   backgroundThreads = Executors.newCachedThreadPool();
  return backgroundThreads;
 }

 private IdentityHashMap<Activity, Object> activities = new IdentityHashMap<Activity, Object>();
 private ExecutorService backgroundThreads;
}
