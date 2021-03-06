package pois.home.a1;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class SendLoveActivity extends Activity implements View.OnClickListener {
        private static String TAG = "SendLoveActivity";

        private EditText mEditTextName;
        private EditText mEditTextMessage;
//        private URI mTextViewResult;

        private static final int PICK_FROM_CAMERA = 0;
        private static final int PICK_FROM_ALBUM = 1;
        private static final int CROP_FROM_iMAGE = 2;


        private Uri mImageCaptureUri;
        private ImageView iv_UserPhoto;
        private int id_view;
        private String absoultePath;

        //private DB_Manger dbmanger;

        @Override
        public void onCreate(Bundle savedInstanceState)
                {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.send_love);
        //        dbmanger = new DB_Manger();
                iv_UserPhoto = (ImageView) this.findViewById(R.id.user_image);
                Button btn_agreeJoin = (Button) this.findViewById(R.id.btn_UploadPicture);
                mEditTextName = (EditText)findViewById(R.id.name);
                mEditTextMessage = (EditText)findViewById(R.id.message);
//                mTextViewResult = (URI) findViewById(R.id.user_image);

                Button buttonInsert = (Button)findViewById(R.id.btn_signupfinish);
                buttonInsert.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                                String name = mEditTextName.getText().toString();
                                String Message = mEditTextMessage.getText().toString();

                                InsertData task = new InsertData();
                                task.execute(name,Message);


                                mEditTextName.setText("");
                                mEditTextMessage.setText("");

                        }
                });
                }






        /**
         * 카메라에서 사진 촬영
         */

        public void doTakePhotoAction() // 카메라 촬영 후 이미지 가져오기
                {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                // 임시로 사용할 파일의 경로를 생성
                String url = "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg";
                mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), url));

                intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
                startActivityForResult(intent, PICK_FROM_CAMERA);
                }

        /**
         * 앨범에서 이미지 가져오기
         */

        public void doTakeAlbumAction() // 앨범에서 이미지 가져오기
                {
                // 앨범 호출
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, PICK_FROM_ALBUM);
                }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
                super.onActivityResult(requestCode,resultCode,data);

                if(resultCode != RESULT_OK) return;
                switch(requestCode)
                {
                        case PICK_FROM_ALBUM:
                                {
                                        // 이후의 처리가 카메라와 같으므로 일단  break없이 진행합니다.
                                        // 실제 코드에서는 좀더 합리적인 방법을 선택하시기 바랍니다.
                                        mImageCaptureUri = data.getData();
                                        Log.d("SmartWheel",mImageCaptureUri.getPath().toString());
                                }
                        case PICK_FROM_CAMERA:
                                {
                                // 이미지를 가져온 이후의 리사이즈할 이미지 크기를 결정합니다.
                                // 이후에 이미지 크롭 어플리케이션을 호출하게 됩니다.
                                Intent intent = new Intent("com.android.camera.action.CROP");
                                intent.setDataAndType(mImageCaptureUri, "image/*");

                                // CROP할 이미지를 200*200 크기로 저장
                                intent.putExtra("outputX", 200); // CROP한 이미지의 x축 크기
                                intent.putExtra("outputY", 200); // CROP한 이미지의 y축 크기
                                intent.putExtra("aspectX", 1); // CROP 박스의 X축 비율
                                intent.putExtra("aspectY", 1); // CROP 박스의 Y축 비율
                                intent.putExtra("scale", true);
                                intent.putExtra("return-data", true);
                                startActivityForResult(intent, CROP_FROM_iMAGE); // CROP_FROM_CAMERA case문 이동
                                break;
                                }
                        case CROP_FROM_iMAGE:
                                {
                                // 크롭이 된 이후의 이미지를 넘겨 받습니다.
                                // 이미지뷰에 이미지를 보여준다거나 부가적인 작업 이후에
                                // 임시 파일을 삭제합니다.
                                if(resultCode != RESULT_OK) {
                                return;
                                }

                final Bundle extras = data.getExtras();

                // CROP된 이미지를 저장하기 위한 FILE 경로
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+
                "/image/"+System.currentTimeMillis()+".jpg";

                if(extras != null)
                        {
                        Bitmap photo = extras.getParcelable("data"); // CROP된 BITMAP
                        iv_UserPhoto.setImageBitmap(photo); // 레이아웃의 이미지칸에 CROP된 BITMAP을 보여줌
                        storeCropImage(photo, filePath); // CROP된 이미지를 외부저장소, 앨범에 저장한다.
                        absoultePath = filePath;
                        break;
                        }
                // 임시 파일 삭제
                File f = new File(mImageCaptureUri.getPath());
                if(f.exists())
                        {
                        f.delete();
                        }
                        }
                        }

        }

        @Override
        public void onClick(View v) {
                id_view = v.getId();
                if(v.getId() == R.id.btn_signupfinish) {
                /** SharedPreference 환경 변수 사용 **/
                SharedPreferences prefs = getSharedPreferences("login", 0);
                /** prefs.getString() return값이 null이라면 2번째 함수를 대입한다. **/
                String login = prefs.getString("USER_LOGIN", "LOGOUT");
//                String facebook_login = prefs.getString("FACEBOOK_LOGIN", "LOGOUT");
                String user_id = prefs.getString("USER_ID","");
                String user_name = prefs.getString("USER_NAME", "");
                String user_password = prefs.getString("USER_PASSWORD", "");
                String user_phone = prefs.getString("USER_PHONE", "");
                String user_email = prefs.getString("USER_EMAIL", "");
        //        dbmanger.select(user_id,user_name,user_password, user_phone, user_email);
        //        dbmanger.selectPhoto(user_name, mImageCaptureUri, absoultePath);
        //

        //        Intent mainIntent = new Intent(SignUpPhotoActivity.this, LoginActivity.class);
        //        SignUpPhotoActivity.this.startActivity(mainIntent);
        //        SignUpPhotoActivity.this.finish();

                Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();

                }else if(v.getId() == R.id.btn_UploadPicture) {
                DialogInterface.OnClickListener cameraListener = new DialogInterface.OnClickListener() {

        @Override

        public void onClick(DialogInterface dialog, int which) {
                doTakePhotoAction();
                }

                };
                DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener() {

        @Override

        public void onClick(DialogInterface dialog, int which) {
                doTakeAlbumAction();
                }

                };

                DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {

        @Override

        public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                }

                };

                new AlertDialog.Builder(this)
                .setTitle("업로드할 이미지 선택")
                .setPositiveButton("사진촬영", cameraListener)
                .setNeutralButton("앨범선택", albumListener)
                .setNegativeButton("취소", cancelListener)
                .show();
                }

                }

           /*
            * Bitmap을 저장하는 부분
            */

        private void storeCropImage(Bitmap bitmap, String filePath) {
                // SmartWheel 폴더를 생성하여 이미지를 저장하는 방식이다.
                String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/SmartWheel";
                File directory_SmartWheel = new File(dirPath);

                if(!directory_SmartWheel.exists()) // SmartWheel 디렉터리에 폴더가 없다면 (새로 이미지를 저장할 경우에 속한다.)
                directory_SmartWheel.mkdir();

                File copyFile = new File(filePath);
                BufferedOutputStream out = null;

                try {
                copyFile.createNewFile();
                out = new BufferedOutputStream(new FileOutputStream(copyFile));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                // sendBroadcast를 통해 Crop된 사진을 앨범에 보이도록 갱신한다.
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(copyFile)));
                out.flush();
                out.close();
                } catch (Exception e) {
                e.printStackTrace();
                }
                }
        class InsertData extends AsyncTask<String, Void, String>{
                ProgressDialog progressDialog;

                @Override
                protected void onPreExecute() {
                        super.onPreExecute();

//                        progressDialog = ProgressDialog.show(this, "Please Wait", null, true, true);
                }


                @Override
                protected void onPostExecute(String result) {
                        super.onPostExecute(result);

                        progressDialog.dismiss();
//                                        mTextViewResult.setText(result);
                        Log.d(TAG, "POST response  - " + result);
                }


                @Override
                protected String doInBackground(String... params) {

                        String name = (String)params[0];
                        String Message = (String)params[1];

                        String serverURL = "https:/pois.000webhestapp.com/addnew.php";
                        String postParameters = "userTel=" + name + "&userMSG=" + Message;


                        try {

                                URL url = new URL(serverURL);
                                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                                httpURLConnection.setReadTimeout(5000);
                                httpURLConnection.setConnectTimeout(5000);
                                httpURLConnection.setRequestMethod("POST");
                                //httpURLConnection.setRequestProperty("content-type", "application/json");
                                httpURLConnection.setDoInput(true);
                                httpURLConnection.connect();


                                OutputStream outputStream = httpURLConnection.getOutputStream();
                                outputStream.write(postParameters.getBytes("UTF-8"));
                                outputStream.flush();
                                outputStream.close();


                                int responseStatusCode = httpURLConnection.getResponseCode();
                                Log.d(TAG, "POST response code - " + responseStatusCode);

                                InputStream inputStream;
                                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                                        inputStream = httpURLConnection.getInputStream();
                                }
                                else{
                                        inputStream = httpURLConnection.getErrorStream();
                                }


                                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                                StringBuilder sb = new StringBuilder();
                                String line = null;

                                while((line = bufferedReader.readLine()) != null){
                                        sb.append(line);
                                }


                                bufferedReader.close();


                                return sb.toString();


                        } catch (Exception e) {

                                Log.d(TAG, "InsertData: Error ", e);

                                return new String("Error: " + e.getMessage());
                        }

                }
        }



}


