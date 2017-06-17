package arabiannight.tistory.com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class TestImageCropActivity extends Activity {

	private static final String TAG = "TestImageCropActivity";

	private static final int PICK_FROM_CAMERA = 0;
	private static final int PICK_FROM_ALBUM = 1;
	private static final int CROP_FROM_CAMERA = 2;

	private Uri mImageCaptureUri;
	private AlertDialog mDialog;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		setLayout();
		
	}

	/** 
	 Button Click 
	 */
	public void onButtonClick(View v){
		switch (v.getId()) {
		case R.id.btn_sns:
			sendSMS("01000000000" , "hi nice to meet you");
			break;
		case R.id.btn_mms:
			Log.e(TAG, "mImageCaptureUri = " + mImageCaptureUri);
			sendMMS(mImageCaptureUri);
//			sendMMSG();
			break;
		case R.id.btn_image_crop:
			mDialog = createDialog();
			mDialog.show();
			break;
		}
	}

	/** 
	 * 다이얼로그 생성
	 */
	private AlertDialog createDialog() {
		final View innerView = getLayoutInflater().inflate(R.layout.image_crop_row, null);

		Button camera = (Button)innerView.findViewById(R.id.btn_camera_crop);
		Button gellary = (Button)innerView.findViewById(R.id.btn_gellary_crop);

		camera.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doTakePhotoAction();
				setDismiss(mDialog);
			}
		});

		gellary.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doTakeAlbumAction();
				setDismiss(mDialog);
			}
		});

		AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setTitle("이미지 Crop");
		ab.setView(innerView);

		return  ab.create();
	}

	/** 
	 * 다이얼로그 종료 
	 */
	private void setDismiss(AlertDialog dialog){
		if(dialog!=null&&dialog.isShowing()){
			dialog.dismiss();
		}
	}

	/** 
	 * SMS 발송	 
	 */
	private void sendSMS(String reciver , String content){
		Uri uri = Uri.parse("smsto:"+reciver);   
		Intent it = new Intent(Intent.ACTION_SENDTO, uri);   
		it.putExtra("sms_body", content);   
		startActivity(it);  
	}
	
	/** 
	 * MMS 발송	(APP TAB BOX) 
	 */
	private void sendMMS(Uri uri){
		uri = Uri.parse(""+uri);   
		Intent it = new Intent(Intent.ACTION_SEND);   
		it.putExtra("sms_body", "some text");   
		it.putExtra(Intent.EXTRA_STREAM, uri);   
		it.setType("image/*");
		// 삼성 단말에서만 허용 ( 앱 선택 박스 없이 호출 )
//		it.setComponent(new ComponentName("com.sec.mms", "com.sec.mms.Mms"));
		startActivity(it); 
	}
	
	/**
	 * MMS 발송 ( 첨부 파일 없음 )
	 */
	private void sendMMSG(){
		Uri mmsUri = Uri.parse("mmsto:");
		Intent sendIntent = new Intent(Intent.ACTION_VIEW, mmsUri);  
		sendIntent.addCategory("android.intent.category.DEFAULT");
		sendIntent.addCategory("android.intent.category.BROWSABLE");
		sendIntent.putExtra("address", "01000000000");
		sendIntent.putExtra("exit_on_sent", true);
		sendIntent.putExtra("subject", "dfdfdf");
		sendIntent.putExtra("sms_body", "dfdfsdf");
		Uri dataUri = Uri.parse(""+mImageCaptureUri);
		sendIntent.putExtra(Intent.EXTRA_STREAM, dataUri);

		startActivity(sendIntent); 
	}
	

	/**
	 * 카메라 호출 하기
	 */
	private void doTakePhotoAction()
	{
		Log.i(TAG, "doTakePhotoAction()");
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Crop된 이미지를 저장할 파일의 경로를 생성
		mImageCaptureUri = createSaveCropFile();
		intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
		startActivityForResult(intent, PICK_FROM_CAMERA);
	}

	/**
	 * 앨범 호출 하기
	 */
	private void doTakeAlbumAction()
	{
		Log.i(TAG, "doTakeAlbumAction()");
		// 앨범 호출
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
		startActivityForResult(intent, PICK_FROM_ALBUM);
	}

	/**
	 * Result Code
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "onActivityResultX");
		if(resultCode != RESULT_OK)
		{
			return;
		}

		switch(requestCode)
		{
		case PICK_FROM_ALBUM:
		{
			Log.d(TAG, "PICK_FROM_ALBUM");
			
			// 이후의 처리가 카메라와 같으므로 일단  break없이 진행합니다.
			// 실제 코드에서는 좀더 합리적인 방법을 선택하시기 바랍니다.
			mImageCaptureUri = data.getData();
			File original_file = getImageFile(mImageCaptureUri);
			
			mImageCaptureUri = createSaveCropFile();
			File cpoy_file = new File(mImageCaptureUri.getPath()); 

			// SD카드에 저장된 파일을 이미지 Crop을 위해 복사한다.
			copyFile(original_file , cpoy_file);
		}

		case PICK_FROM_CAMERA:
		{
			Log.d(TAG, "PICK_FROM_CAMERA"); 

			// 이미지를 가져온 이후의 리사이즈할 이미지 크기를 결정합니다.
			// 이후에 이미지 크롭 어플리케이션을 호출하게 됩니다.

			Intent intent = new Intent("com.android.camera.action.CROP");
			intent.setDataAndType(mImageCaptureUri, "image/*"); 
			
			// Crop한 이미지를 저장할 Path
			intent.putExtra("output", mImageCaptureUri);
			
			// Return Data를 사용하면 번들 용량 제한으로 크기가 큰 이미지는
			// 넘겨 줄 수 없다.
//			intent.putExtra("return-data", true); 
			startActivityForResult(intent, CROP_FROM_CAMERA);

			break;
		}
		
		case CROP_FROM_CAMERA:
		{
			Log.w(TAG, "CROP_FROM_CAMERA");
			
			// Crop 된 이미지를 넘겨 받습니다.
			Log.w(TAG, "mImageCaptureUri = " + mImageCaptureUri);

			String full_path = mImageCaptureUri.getPath();
			String photo_path = full_path.substring(4, full_path.length());
			
			Log.w(TAG, "비트맵 Image path = "+photo_path);
			
			Bitmap photo = BitmapFactory.decodeFile(photo_path);
			mPhotoImageView.setImageBitmap(photo);

			break;
		}
		}
	}
	
	/**
	 * Crop된 이미지가 저장될 파일을 만든다.
	 * @return Uri
	 */
	private Uri createSaveCropFile(){
		Uri uri;
		String url = "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg";
		uri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), url));
		return uri;
	}


	/**
	 * 선택된 uri의 사진 Path를 가져온다.
	 * uri 가 null 경우 마지막에 저장된 사진을 가져온다.
	 * @param uri
	 * @return
	 */
	private File getImageFile(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		if (uri == null) {
			uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		}

		Cursor mCursor = getContentResolver().query(uri, projection, null, null, 
				MediaStore.Images.Media.DATE_MODIFIED + " desc");
		if(mCursor == null || mCursor.getCount() < 1) {
			return null; // no cursor or no record
		}
		int column_index = mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		mCursor.moveToFirst();

		String path = mCursor.getString(column_index);

		if (mCursor !=null ) {
			mCursor.close();
			mCursor = null;
		}

		return new File(path);
	}

	/**
	 * 파일 복사
	 * @param srcFile : 복사할 File
	 * @param destFile : 복사될 File
	 * @return
	 */
	public static boolean copyFile(File srcFile, File destFile) {
		boolean result = false;
		try {
			InputStream in = new FileInputStream(srcFile);
			try {
				result = copyToFile(in, destFile);
			} finally  {
				in.close();
			}
		} catch (IOException e) {
			result = false;
		}
		return result;
	}

	/**
	 * Copy data from a source stream to destFile.
	 * Return true if succeed, return false if failed.
	 */
	private static boolean copyToFile(InputStream inputStream, File destFile) {
		try {
			OutputStream out = new FileOutputStream(destFile);
			try {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) >= 0) {
					out.write(buffer, 0, bytesRead);
				}
			} finally {
				out.close();
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/*
	 * Layout
	 */
	private ImageView mPhotoImageView;

	private void setLayout(){
		mPhotoImageView = (ImageView)findViewById(R.id.img_bitmap);
	}
}