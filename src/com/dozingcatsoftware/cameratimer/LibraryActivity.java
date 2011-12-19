package com.dozingcatsoftware.cameratimer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class LibraryActivity extends Activity {
	
	static int CELL_WIDTH = 92;
	static int CELL_HEIGHT = 69;
	
	String imageDirectory;
	
	GridView gridView;
	int selectedGridIndex;
	
	List imageMaps = new ArrayList();
	static String IMAGE_URI_KEY = "imageUri";
	static String IMAGE_FILENAME_KEY = "imageFilename";
	
	// A cache of scaled Bitmaps for the image files, so we can avoid reloading them as the user scrolls.
	ScaledBitmapCache bitmapCache;
	
	public static Intent intentWithImageDirectory(Context parent, String imageDirectory) {
		Intent intent = new Intent(parent, LibraryActivity.class);
		intent.putExtra("imageDirectory", imageDirectory);
		return intent;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.library_list);
		
		imageDirectory = getIntent().getStringExtra("imageDirectory");
		bitmapCache = new ScaledBitmapCache(this, imageDirectory);
		
		gridView = (GridView) findViewById(R.id.gridview);
		gridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View view, int position, long id) {
				selectedGridIndex = position;
				ViewImageActivity.startActivityWithImageURI(LibraryActivity.this, 
						(Uri)((Map)imageMaps.get(position)).get(IMAGE_URI_KEY), "image/jpeg");
			}
		});
		readImageThumbnails();
		displayGrid();
    }
	
	void readImageThumbnails() {
		List<String> filenames = Collections.emptyList();
		File dir = new File(imageDirectory);
		if (dir.isDirectory()) {
			filenames = Arrays.asList(dir.list());
		}
		Collections.sort(filenames);
		Collections.reverse(filenames);

		imageMaps.clear();
		for(String fname : filenames) {
			if (fname.endsWith(".jpg")) {
				Uri imageUri = Uri.fromFile(new File(imageDirectory + File.separator + fname));
				Map dmap = new HashMap();
				dmap.put(IMAGE_URI_KEY, imageUri);
				dmap.put(IMAGE_FILENAME_KEY, fname);
				imageMaps.add(dmap);
			}
		}
	}
	
	void displayGrid() {
		SimpleAdapter adapter = new SimpleAdapter(this, imageMaps, 
				R.layout.library_cell, 
				new String[] {IMAGE_FILENAME_KEY}, 
				new int[] {R.id.grid_image});
		adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
			public boolean setViewValue(View view, Object data, String textRepresentation) {
				String filename = (String)data;
				Bitmap bitmap = bitmapCache.getScaledBitmap(filename, CELL_WIDTH, CELL_HEIGHT);
				((ImageView)view).setImageBitmap(bitmap);
				return true;
			}
		});
		gridView.setAdapter(adapter);
		
		// show text message if no images available
		View noImagesView = findViewById(R.id.noImagesTextView);
		noImagesView.setVisibility(imageMaps.size()>0 ? View.GONE : View.VISIBLE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode==ViewImageActivity.DELETE_RESULT) {
			bitmapCache.removeFilename((String)((Map)imageMaps.get(selectedGridIndex)).get(IMAGE_FILENAME_KEY));
			imageMaps.remove(selectedGridIndex);
			displayGrid();
		}
	}

}
