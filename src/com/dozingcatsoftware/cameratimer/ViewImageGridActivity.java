package com.dozingcatsoftware.cameratimer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.util.AndroidUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

public class ViewImageGridActivity extends Activity {
	
	List<Uri> imageURIs;
	List<PictureView> imageViews;
	List<View> deleteButtons;
	List<View> viewButtons;
	
	int viewImageIndex;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.imagegrid);
        
        imageURIs = getIntent().getParcelableArrayListExtra("images");
        imageViews = Arrays.asList((PictureView)findViewById(R.id.gridImageView1), (PictureView)findViewById(R.id.gridImageView2), 
        		(PictureView)findViewById(R.id.gridImageView3), (PictureView)findViewById(R.id.gridImageView4));
        
        for(int i=0; i<imageURIs.size(); i++) {
        	imageViews.get(i).setImageFromURI(imageURIs.get(i));
        }
        
        deleteButtons = Arrays.asList(findViewById(R.id.gridDeleteButton1), findViewById(R.id.gridDeleteButton2),
        		findViewById(R.id.gridDeleteButton3), findViewById(R.id.gridDeleteButton4));
        AndroidUtils.bindOnClickListener(this, deleteButtons.get(0), "deleteImage0");
        AndroidUtils.bindOnClickListener(this, deleteButtons.get(1), "deleteImage1");
        AndroidUtils.bindOnClickListener(this, deleteButtons.get(2), "deleteImage2");
        AndroidUtils.bindOnClickListener(this, deleteButtons.get(3), "deleteImage3");
        
        viewButtons = Arrays.asList(findViewById(R.id.gridViewButton1), findViewById(R.id.gridViewButton2), 
        		findViewById(R.id.gridViewButton3), findViewById(R.id.gridViewButton4));
        AndroidUtils.bindOnClickListener(this, viewButtons.get(0), "viewImage0");
        AndroidUtils.bindOnClickListener(this, viewButtons.get(1), "viewImage1");
        AndroidUtils.bindOnClickListener(this, viewButtons.get(2), "viewImage2");
        AndroidUtils.bindOnClickListener(this, viewButtons.get(3), "viewImage3");
    }

    public static Intent startActivityWithImageURIs(Context parent, List<Uri> imageURIs) {
    	Intent intent = new Intent(parent, ViewImageGridActivity.class);
    	intent.putParcelableArrayListExtra("images", new ArrayList(imageURIs));
    	parent.startActivity(intent);
    	return intent;
    }

    // launch gallery and terminate this activity, so when gallery activity finishes user will go back to main activity
    public void viewImageInGallery(int imageNum) {
    	viewImageIndex = imageNum; // so we know which image to delete if needed
    	ViewImageActivity.startActivityWithImageURI(this, imageURIs.get(imageNum), "image/jpeg");
    }
    
    public void viewImage0() {viewImageInGallery(0);}
    public void viewImage1() {viewImageInGallery(1);}
    public void viewImage2() {viewImageInGallery(2);}
    public void viewImage3() {viewImageInGallery(3);}
    
    public void deleteImage(int imageNum) {
    	String path = imageURIs.get(imageNum).getPath();
    	(new File(path)).delete();
    	imageViews.get(imageNum).setImageFromURI(null);
    	
    	// exit activity if all images deleted
    	boolean allDeleted = true;
    	for(PictureView pv : imageViews) {
    		if (pv.getImageURI()!=null) allDeleted = false;
    	}
    	if (allDeleted) {
    		this.finish();
    	}
    	else {
    		deleteButtons.get(imageNum).setVisibility(View.INVISIBLE);
    		viewButtons.get(imageNum).setVisibility(View.INVISIBLE);
        	imageViews.get(imageNum).invalidate();
    	}
    }

    public void deleteImage0() {deleteImage(0);}
    public void deleteImage1() {deleteImage(1);}
    public void deleteImage2() {deleteImage(2);}
    public void deleteImage3() {deleteImage(3);}
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode==ViewImageActivity.DELETE_RESULT) {
			deleteImage(viewImageIndex);
		}
	}

}
