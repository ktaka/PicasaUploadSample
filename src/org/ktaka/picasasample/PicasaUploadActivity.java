/*
 * Copyright (c) 2013 Kenichi Takahashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ktaka.picasasample;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import org.ktaka.api.services.picasa.PicasaClient;
import org.ktaka.api.services.picasa.PicasaUrl;
import org.ktaka.api.services.picasa.model.AlbumEntry;
import org.ktaka.api.services.picasa.model.AlbumFeed;
import org.ktaka.api.services.picasa.model.GmlPoint;
import org.ktaka.api.services.picasa.model.PhotoEntry;
import org.ktaka.api.services.picasa.model.TagEntry;
import org.ktaka.api.services.picasa.model.UserFeed;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.View;


public class PicasaUploadActivity extends Activity {

	/**
	 * Be sure to specify the name of your application. If the application name is {@code null} or
	 * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
	 */
	private static final String APPLICATION_NAME = "PicasaUploadTest";

	/** Global instance of the HTTP transport. */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	static final String TAG = "PicasaUploadActivity";
	
	static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
	static final int REQUEST_AUTHORIZATION = 1;
	static final int REQUEST_ACCOUNT_PICKER = 2;
	static final int REQUEST_PICKED_FROM_GALLERY = 3;
	static final int REQUEST_PICKED_FROM_CAMERA = 4;

	private static final String PREF_ACCOUNT_NAME = "accountName";
	GoogleAccountCredential credential;
	PicasaClient client;
	UserFeed feed;
	Uri imgFileUri;
	
	int numAsyncTasks;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_picasa_upload);
		
	    credential =
	            GoogleAccountCredential.usingOAuth2(this, Collections.singleton(PicasaUrl.ROOT_URL));
	        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
	        credential.setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
        client = new PicasaClient(HTTP_TRANSPORT.createRequestFactory(credential));
        client.setApplicationName(APPLICATION_NAME);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.picasa_upload, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (checkGooglePlayServicesAvailable()) {
			haveGooglePlayServices();
		}
	}
	  
	private AlbumEntry postAlbum(UserFeed feed) throws IOException {
		System.out.println();
		AlbumEntry newAlbum = new AlbumEntry();
		newAlbum.access = "public";
		newAlbum.title = "A new album";
		newAlbum.summary = "Test for MiraiKioku API";
		AlbumEntry album = client.executeInsert(feed, newAlbum);
		showAlbum(album);
		return album;
	}

	private void addTagToPhoto(String feedUrl, String tag) throws IOException {
		TagEntry tagEntry = new TagEntry();
		tagEntry.title = tag;
		PicasaUrl url = new PicasaUrl(feedUrl);
		client.executeAddTagToPhoto(url, tagEntry);
	}
	
	@SuppressWarnings("unused")
	private PhotoEntry postPhoto(AlbumEntry album, Uri uri) throws IOException {
	    String fileName = "test";
		InputStream iStream = getContentResolver().openInputStream(uri);
	    InputStreamContent content = new InputStreamContent("image/jpeg", iStream);
	    PhotoEntry photo =
	        client.executeInsertPhotoEntry(new PicasaUrl(album.getFeedLink()), content, fileName);
		Log.i("TAG", "Image URL: " + photo.mediaGroup.content.url);
	    return photo;
	}

	@SuppressWarnings("unused")
	private PhotoEntry postPhotoWithMetaData(AlbumEntry album, Uri uri) throws IOException {
		// NOTE: this video is not included in the sample
		InputStream iStream = getContentResolver().openInputStream(uri);
	    InputStreamContent imgContent = new InputStreamContent("image/jpeg", iStream);
		PhotoEntry photo = new PhotoEntry();
		photo.title = "未来へのキオクのテスト";
		photo.summary =  "未来へのキオクへの upload API のテストです。";
		GmlPoint point = GmlPoint.createLatLon(35.626446, 139.723444);
		PhotoEntry result = client.executeInsertPhotoEntryWithMetadata(
				photo, new PicasaUrl(album.getFeedLink()), imgContent, point);
		Log.i(TAG, "Image URL with Metadata: " + result.mediaGroup.content.url);
		return result;
	}
	
	UserFeed showAlbums() throws IOException {
		// build URL for the default user feed of albums
		PicasaUrl url = PicasaUrl.relativeToRoot("feed/api/user/default");
		// execute GData request for the feed
		feed = client.executeGetUserFeed(url);
		System.out.println("User: " + feed.author.name);
		System.out.println("Total number of albums: " + feed.totalResults);
		// show albums
		if (feed.albums != null) {
			for (AlbumEntry album : feed.albums) {
				showAlbum(album);
			}
		}
		return feed;
	}
	
	private void showAlbum(AlbumEntry album) throws IOException {
		System.out.println();
		System.out.println("-----------------------------------------------");
		System.out.println("Album title: " + album.title);
		System.out.println("Updated: " + album.updated);
		System.out.println("Album ETag: " + album.etag);
		if (album.summary != null) {
			System.out.println("Description: " + album.summary);
		}
		if (album.numPhotos != 0) {
			System.out.println("Total number of photos: " + album.numPhotos);
			PicasaUrl url = new PicasaUrl(album.getFeedLink());
			AlbumFeed feed = client.executeGetAlbumFeed(url);
			for (PhotoEntry photo : feed.photos) {
				System.out.println();
				System.out.println("Photo title: " + photo.title);
				if (photo.summary != null) {
					System.out.println("Photo description: " + photo.summary);
				}
				System.out.println("editLink: " + photo.getEditLink());
				System.out.println("feedLink: " + photo.getFeedLink());
				System.out.println("Image MIME type: " + photo.mediaGroup.content.type);
				System.out.println("Image URL: " + photo.mediaGroup.content.url);
			}
		}
	}

	private void chooseAccount() {
		startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
	      case REQUEST_GOOGLE_PLAY_SERVICES:
	          if (resultCode == Activity.RESULT_OK) {
	            haveGooglePlayServices();
	          } else {
	            checkGooglePlayServicesAvailable();
	          }
	          break;
		case REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK) {
				AsyncLoadTasks.run(this);
			} else {
				chooseAccount();
			}
			break;
		case REQUEST_ACCOUNT_PICKER:
			if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
				String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					credential.setSelectedAccountName(accountName);
					SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString(PREF_ACCOUNT_NAME, accountName);
					editor.commit();
					AsyncLoadTasks.run(this);
				}
			} else if (resultCode == Activity.RESULT_CANCELED) {
				finish();
			}
			break;
		case REQUEST_PICKED_FROM_GALLERY:
			Uri uri = data.getData();
			Log.d(TAG, "url=" + uri);
			AsyncUploader.run(this, uri);
			break;
		case REQUEST_PICKED_FROM_CAMERA:
			if(data != null) {
		        Log.e("Intent value:", data.toString());
		        imgFileUri = data.getData();
		    }

			Log.d(TAG, "url=" + imgFileUri);
			AsyncUploader.run(this, imgFileUri);
			break;
			
		}
	}
	
	public static String getPath(Context context, Uri uri) {
	    ContentResolver contentResolver = context.getContentResolver();
	    String[] columns = { MediaStore.Images.Media.DATA };
	    Cursor cursor = contentResolver.query(uri, columns, null, null, null);
	    cursor.moveToFirst();
	    String path = cursor.getString(0);
	    cursor.close();
	    return path;
	}
	
	static class GPS {
	    private static StringBuilder sb = new StringBuilder(20);

	    /**
	     * returns ref for latitude which is S or N.
	     * @param latitude
	     * @return S or N
	     */
	    public static String latitudeRef(double latitude) {
	        return latitude<0.0d?"S":"N";
	    }

	    /**
	     * returns ref for latitude which is S or N.
	     * @param latitude
	     * @return S or N
	     */
	    public static String longitudeRef(double longitude) {
	        return longitude<0.0d?"W":"E";
	    }

	    /**
	     * convert latitude into DMS (degree minute second) format. For instance<br/>
	     * -79.948862 becomes<br/>
	     *  79/1,56/1,55903/1000<br/>
	     * It works for latitude and longitude<br/>
	     * @param latitude could be longitude.
	     * @return
	     */
	    synchronized public static final String convert(double latitude) {
	        latitude=Math.abs(latitude);
	        int degree = (int) latitude;
	        latitude *= 60;
	        latitude -= (degree * 60.0d);
	        int minute = (int) latitude;
	        latitude *= 60;
	        latitude -= (minute * 60.0d);
	        int second = (int) (latitude*1000.0d);

	        sb.setLength(0);
	        sb.append(degree);
	        sb.append("/1,");
	        sb.append(minute);
	        sb.append("/1,");
	        sb.append(second);
	        sb.append("/1000,");
	        return sb.toString();
	    }
	}
	
	void addGeolocationToExif(Uri uri, double lat, double lon) throws IOException {
		String fileName = getPath(this, uri);
		ExifInterface exif = new ExifInterface(fileName);
		exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPS.convert(lat));
		exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPS.latitudeRef(lat));
		exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPS.convert(lon));
		exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPS.longitudeRef(lon));
		exif.saveAttributes();
	}
	
	void uploadPhoto(Uri uri) {
		try {
			AlbumEntry album =  postAlbum(feed);
			//addGeolocationToExif(uri, 35.607267, 140.106291);
			//PhotoEntry photo = postPhoto(album, uri);
			PhotoEntry photo = postPhotoWithMetaData(album, uri);
			String feedLink = photo.getFeedLink();
			addTagToPhoto(feedLink, "foo");
			Log.i(TAG, feedLink + " was uploaded.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
		runOnUiThread(new Runnable() {
			public void run() {
				Dialog dialog =
						GooglePlayServicesUtil.getErrorDialog(connectionStatusCode, PicasaUploadActivity.this,
								REQUEST_GOOGLE_PLAY_SERVICES);
				dialog.show();
			}
		});
	}
	  
	/** Check that Google Play services APK is installed and up to date. */
	private boolean checkGooglePlayServicesAvailable() {
		final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
			showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
			return false;
		}
		return true;
	}

	private void haveGooglePlayServices() {
		// check if there is already an account selected
		if (credential.getSelectedAccountName() == null) {
			// ask user to choose account
			chooseAccount();
		} else {
			// load calendars
			AsyncLoadTasks.run(this);
		}
	}

	public void openCamera(View v) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		imgFileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
		Log.d(TAG, "openCamera with url=" + imgFileUri);
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, imgFileUri); // set the image file name

	    // start the image capture Intent
	    startActivityForResult(intent, REQUEST_PICKED_FROM_CAMERA);
	}
	
	public void showGallery(View v) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, REQUEST_PICKED_FROM_GALLERY);
	}
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "MyCameraApp");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("MyCameraApp", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    if (imgFileUri != null) {
	        outState.putString("cameraImageUri", imgFileUri.toString());
	    }
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
	    super.onRestoreInstanceState(savedInstanceState);
	    if (savedInstanceState.containsKey("cameraImageUri")) {
	        imgFileUri = Uri.parse(savedInstanceState.getString("cameraImageUri"));
	    }
	}
}
