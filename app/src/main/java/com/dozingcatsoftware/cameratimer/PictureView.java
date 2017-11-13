package com.dozingcatsoftware.cameratimer;

import com.dozingcatsoftware.util.AndroidUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;

public class PictureView extends View {

	public PictureView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	Bitmap bitmap;
	float cornerImageRatio = 1.0f;
	boolean drawBorder = false;
	Uri imageURI, lastImageURI;
	
	public void setImageFromURI(Uri newImageURI) {
		this.lastImageURI = this.imageURI;
		this.imageURI = newImageURI;
		if (this.imageURI==null) {
			this.bitmap = null;
		}
		this.invalidate();
	}
	
	public Uri getImageURI() {
		return imageURI;
	}
	
	@Override
	protected void onDraw (Canvas canvas) {
		// has to be done here because in setImageFromURI reported view size is still 0??
		if (imageURI!=null) {
			if (!imageURI.equals(lastImageURI)) {
				try {
					Rect imageRect = rectForImage();
					bitmap = AndroidUtils.scaledBitmapFromURIWithMinimumSize(this.getContext(), imageURI, imageRect.width(), imageRect.height());
				}
				catch(Exception ex) {
					bitmap = null;
				}
			}
			if (bitmap!=null) {
				Rect imageRect = rectForImage();
				
				if (drawBorder) {
					// draw black and white borders to identify bounds
					Paint cornerEdgePaint = new Paint();
					cornerEdgePaint.setStyle(Paint.Style.STROKE);
					cornerEdgePaint.setARGB(255,0,0,0);
					canvas.drawRect(imageRect, cornerEdgePaint);
					
					cornerEdgePaint.setARGB(255,255,255,255);
					imageRect.left += 1;
					imageRect.bottom -= 1;
					canvas.drawRect(imageRect, cornerEdgePaint);

					imageRect.left += 1;
					imageRect.bottom -= 1;
				}
				canvas.drawBitmap(bitmap, null, imageRect, null);
			}
		}
	}

	Rect rectForImage() {
		if (bitmap!=null) {
			float bitmapRatio = 1.0f*bitmap.getWidth() / bitmap.getHeight();
			float viewRatio = 1.0f*this.getWidth() / this.getHeight();
			if (bitmapRatio < viewRatio) {
				// bitmap is taller than view, use full view height and scaled width
				int width = (int)(this.getHeight()*bitmapRatio);
				int xstart = (this.getWidth() - width) / 2;
				return new Rect(xstart, 0, xstart + width, this.getHeight());
			}
			if (bitmapRatio > viewRatio) {
				// bitmap is wider than view, use full view width and scaled height
				int height = (int)(this.getWidth()/bitmapRatio);
				int ystart = (this.getHeight() - height) / 2;
				return new Rect(0, ystart, this.getWidth(), ystart + height);
			}
		}
		return new Rect(0, 0, this.getWidth(), this.getHeight());
	}
	
	public boolean isPointInCornerImage(float x, float y) {
		if (bitmap==null || cornerImageRatio<=0) return false;
		Rect cornerImageRect = rectForImage();
		return cornerImageRect.contains((int)x, (int)y);
	}

}
