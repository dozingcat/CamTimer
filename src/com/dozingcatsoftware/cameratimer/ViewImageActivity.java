package com.dozingcatsoftware.cameratimer;

import java.io.File;

import com.dozingcatsoftware.util.AndroidUtils;
import com.dozingcatsoftware.cameratimer.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Window;
import android.widget.ImageView;

public class ViewImageActivity extends Activity {
	
	public static final int DELETE_RESULT = Activity.RESULT_FIRST_USER;
	
	ImageView imageView;
	Uri imageUri;

    public static Intent startActivityWithImageURI(Activity parent, Uri imageURI, String type) {
    	Intent intent = new Intent(parent, ViewImageActivity.class);
    	intent.setDataAndType(imageURI, type);
    	parent.startActivityForResult(intent, 0);
    	return intent;
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.imageview);
        
        imageView = (ImageView)findViewById(R.id.imageView);
        imageUri = getIntent().getData();
        
        // assume full screen, there's no good way to get notified once layout happens and views have nonzero width/height
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        try {
        	imageView.setImageBitmap(AndroidUtils.scaledBitmapFromURIWithMinimumSize(this, imageUri, 
        			dm.widthPixels, dm.heightPixels));
        }
        catch(Exception ex) {}
        
        AndroidUtils.bindOnClickListener(this, this.findViewById(R.id.deleteImageButton), "deleteImage");
        AndroidUtils.bindOnClickListener(this, this.findViewById(R.id.shareImageButton), "shareImage");
        AndroidUtils.bindOnClickListener(this, this.findViewById(R.id.exitViewImageButton), "goBack");
    }
    
    public void goBack() {
    	this.finish();
    }
    
    public void deleteImage() {
    	String path = this.getIntent().getData().getPath();
    	(new File(path)).delete();
    	this.setResult(DELETE_RESULT);
    	this.finish();
    }
    
    public void shareImage() {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType(this.getIntent().getType());
		shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
		shareIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
		startActivity(Intent.createChooser(shareIntent, "Share Picture Using:"));
    }
    
    // launch gallery and terminate this activity, so when gallery activity finishes user will go back to main activity
    public void viewImageInGallery() {
    	Intent galleryIntent = new Intent(Intent.ACTION_VIEW);
    	galleryIntent.setDataAndType(this.getIntent().getData(), this.getIntent().getType());
    	// FLAG_ACTIVITY_NO_HISTORY tells the OS to not return to the gallery if the user goes to the home screen and relaunches the app
    	galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    	this.startActivity(galleryIntent);
    	this.finish();
    }

}
