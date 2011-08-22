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
	
	ImageView imageView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.imageview);
        
        imageView = (ImageView)findViewById(R.id.imageView);
        
        // assume full screen, there's no good way to get notified once layout happens and views have nonzero width/height
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        try {
        	imageView.setImageBitmap(AndroidUtils.scaledBitmapFromURIWithMinimumSize(this, this.getIntent().getData(), dm.widthPixels, dm.heightPixels));
        }
        catch(Exception ex) {}
        
        AndroidUtils.bindOnClickListener(this, this.findViewById(R.id.deleteImageButton), "deleteImage");
        AndroidUtils.bindOnClickListener(this, this.findViewById(R.id.openGalleryButton), "viewImageInGallery");
        AndroidUtils.bindOnClickListener(this, this.findViewById(R.id.exitViewImageButton), "goBack");
    }
    
    public static Intent startActivityWithImageURI(Context parent, Uri imageURI, String type) {
    	Intent intent = new Intent(parent, ViewImageActivity.class);
    	intent.setDataAndType(imageURI, type);
    	parent.startActivity(intent);
    	return intent;
    }
    
    public void goBack() {
    	this.finish();
    }
    
    public void deleteImage() {
    	String path = this.getIntent().getData().getPath();
    	(new File(path)).delete();
    	this.finish();
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
