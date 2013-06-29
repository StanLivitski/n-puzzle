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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * An activity that displays the application's license
 * and related documents.
 */
public class LicensePages extends Activity
{
 protected static final String CHARSET_UTF_8 = "UTF-8";
 @Override
 protected void onCreate(Bundle savedInstanceState)
 {
  super.onCreate(savedInstanceState);
  page = new WebView(this);
  final WebSettings settings = page.getSettings();
  settings.setBuiltInZoomControls(true);
  settings.setTextSize(WebSettings.TextSize.SMALLEST);
  page.setWebViewClient(WEB_CLIENT);
  setContentView(page);
  findViewById(R.id.text_app_footer_license).setVisibility(View.INVISIBLE);
  loadAssetPage(LICENSE_HOME);
 }

 @Override
 public boolean onKeyDown(int keyCode, KeyEvent event)
 {
  if (keyCode == KeyEvent.KEYCODE_BACK && page.canGoBack())
  {
   page.goBack();
   return true;
  }
  return super.onKeyDown(keyCode, event);
 }

 protected void loadAssetPage(String path)
 {
  try
  {
   final SoftReference<String> ref = cache.get(path);
   String data = null == ref ? null : ref.get();
   if (null == data)
   {
    final byte[] raw = bufferAssetPage(path);
    data = Uri.encode(new String(raw, CHARSET_UTF_8));
    cache.put(path, new SoftReference<String>(data));
   }
   page.loadData(data, contentTypeFor(path) + ";charset=" + CHARSET_UTF_8, null);
  }
  catch (Exception e)
  {
   String msg = "Could not load page from asset file '"
     + path + "'";
   Log.e(LOG_TAG, msg, e);
   alert(msg);
   finish();
  }
 }

 protected byte[] bufferAssetPage(String path)
 	throws IOException
 {
  InputStream in = null;
  ByteArrayOutputStream out = new ByteArrayOutputStream();
  byte[] buf = new byte[16384];
  try
  {
   in = getAssets().open(path);
   for (int read; 0 <= (read = in.read(buf));)
    out.write(buf, 0, read);
   return out.toByteArray();
  }
  finally
  {
   if (null != in)
    try { in.close(); } catch (Exception ignored) {}
  }
 }

 protected String contentTypeFor(String path)
 {
  final int dotAt = path.lastIndexOf('.');
  final String ext = 0 > dotAt ? null : path.substring(dotAt + 1).toLowerCase();
  final String type = MIME_CONTENT_TYPES.get(ext);
  return null == type ? "text/html" : type;
 }

 protected static final String LICENSE_HOME = "NOTICE.html";
 protected static final Map<String, String> MIME_CONTENT_TYPES;
 static
 {
  Map<String, String> types = new HashMap<String, String>();
  types.put("html", "text/html");
  types.put("txt", "text/plain");
  MIME_CONTENT_TYPES = Collections.unmodifiableMap(types);
 }

 private final WebViewClient WEB_CLIENT = new WebViewClient()
 {
  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String url)
  {
   final Uri uri = Uri.parse(url);
   if (uri.isAbsolute() || null != uri.getAuthority())
   {
    Intent call = new Intent(Intent.ACTION_VIEW, uri);
    startActivity(call);
   }
   else
   {
    final String path = uri.getPath();
    loadAssetPage(path.startsWith("/") ? path.substring(1) : path);
   }
   return true;
  }
 };

 private static final String LOG_TAG = "LicensePages";
 private WebView page;
 private Map<String, SoftReference<String>> cache = new HashMap<String, SoftReference<String>>();
}
