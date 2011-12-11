package com.dozingcatsoftware.cameratimer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import com.dozingcatsoftware.util.AndroidUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;

/** This class implements a two-level cache for Bitmaps. The first level is an in-memory map
 * which uses SoftReferences so that the Bitmaps will be freed when necessary. The second
 * level is a thumbnails directory on the SD card, where smaller versions of the images
 * will be saved for faster retrieval.
 */

public class ScaledBitmapCache {
	
	Context context;
	String imageDirectory;
	String thumbnailDirectory = "thumbnails";
	
	Map<String, SoftReference<Bitmap>> scaledBitmapCache = new HashMap<String, SoftReference<Bitmap>>();
	
	public ScaledBitmapCache(Context context, String imageDirectory) {
		this.context = context;
		this.imageDirectory = imageDirectory;
	}
	
	public Bitmap getScaledBitmap(String filename, int minWidth, int minHeight) {
		Bitmap bitmap = null;
		// check in-memory cache
		SoftReference<Bitmap> ref = scaledBitmapCache.get(filename);
		bitmap = (ref!=null) ? ref.get() : null;
		if (bitmap!=null) return bitmap;
		
		// check thumbnail directory
		File thumbfile = (new File(thumbnailPath(filename)));
		if (thumbfile.isFile()) {
			try {
				bitmap = AndroidUtils.scaledBitmapFromURIWithMinimumSize(context, 
						Uri.fromFile(thumbfile), minWidth, minHeight);
				if (bitmap!=null && bitmap.getWidth()>=minWidth && bitmap.getHeight()>=minHeight) {
					scaledBitmapCache.put(filename, new SoftReference(bitmap));
					return bitmap;
				}
			}
			catch(Exception ignored) {}
		}
		
		// read full-size image
		try {
			Uri fullUri = Uri.fromFile(new File(this.imageDirectory + File.separator + filename));
			bitmap = AndroidUtils.scaledBitmapFromURIWithMinimumSize(context, fullUri, minWidth, minHeight);
		}
		catch(Exception ex) {
			bitmap = null;
		}
		if (bitmap!=null) {
			// write to in-memory map and save thumbnail image
			scaledBitmapCache.put(filename, new SoftReference(bitmap));
			try {
				// create thumbnail directory if it doesn't exist
				(new File(thumbnailPath(""))).mkdirs();
				OutputStream thumbnailOutputStream = new FileOutputStream(thumbfile);
				bitmap.compress(CompressFormat.JPEG, 90, thumbnailOutputStream);
				thumbnailOutputStream.close();
			}
			catch(Exception ignored) {}
		}
		return bitmap;
	}
	
	public void removeFilename(String filename) {
		scaledBitmapCache.remove(filename);
		(new File(thumbnailPath(filename))).delete();
	}

	String thumbnailPath(String filename) {
		return this.imageDirectory + File.separator + this.thumbnailDirectory + File.separator + filename;
	}
}
