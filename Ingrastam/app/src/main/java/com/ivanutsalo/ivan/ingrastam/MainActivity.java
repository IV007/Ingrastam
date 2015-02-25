package com.ivanutsalo.ivan.ingrastam;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;


public class MainActivity extends Activity implements ScrollViewListener{

    private static final String CLIENT_ID = "593bf1528f3041b188f7202cb61ff02b";
    private static final String CLIENT_SECRET = "684659d00cac4c6f90b9377648100135";

    public static ArrayList<Bitmap> bitmapList = new ArrayList<Bitmap>();
    private static int smallCounter = 0;
    private static String next_url = "";
    private static int asyncCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    @Override
    protected void onStart() {
        super.onStart();

        //Use AsyncTask to get most recent list of selfies
        new LoadInstagramImages(true).execute("https://api.instagram.com/v1/tags/selfie/media/" +
                "recent?type=image?access_token=1133143413.593bf15.09735f1a0a0048de8cda698db44def09&client_id= " +CLIENT_ID);
        ScrollViewExt scrollViewExt = (ScrollViewExt) findViewById(R.id.scroll_view_instagram);
        scrollViewExt.setScrollViewListener(this);
    }

    public class LoadInstagramImages extends AsyncTask<String, Bitmap, String> {

        ProgressDialog progressDialog;

        private WeakReference<ImageView> imageViewReference;
        boolean noProgressDialog = false;
        public LoadInstagramImages(boolean noProgressDialog) {
            //Create async to decide if there should be a new progressDialog
            this.noProgressDialog = noProgressDialog;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //display Progress Dialog if true
            if(noProgressDialog){
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setTitle("Downloading...");
                progressDialog.setMessage("Please wait...");
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
            }
            //keep track of number of async  for thread mgt
            asyncCounter++;
            Log.d("No of Async", "" +asyncCounter);
        }

        @Override
        protected String doInBackground(String... params) {
            String nextUrl = "";
            try {
                //create a URL connection to download each picture
                URL fetchPic = new URL(params[0]);
                URLConnection connect;
                connect = fetchPic.openConnection();
                BufferedReader bReader = new BufferedReader(new InputStreamReader(connect.getInputStream()));

                String line;
                while((line = bReader.readLine()) != null){
                    //get JSON data from the url link and extract the data
                    JSONObject jObj = new JSONObject(line);

                    //Get the data JSON Array to parse for picture urls
                    JSONArray jArray = jObj.getJSONArray("data");
                    JSONObject paginationObj = jObj.getJSONObject("pagination");

                    //get the next url to download next
                    nextUrl = paginationObj.getString("nextUrl");
                    Log.d("Url", nextUrl);

                    //parse through the array for image links to be downloaded and displayed on the screen
                    for (int i = 0; i < jArray.length(); i++){
                        JSONObject jsonObject = (JSONObject) jArray.get(i);
                        JSONObject imagesJsonObj = jsonObject.getJSONObject("images");

                        //make resolution Standard
                        JSONObject stdResJsonObj = imagesJsonObj.getJSONObject("standard_resolution");
                        String url = stdResJsonObj.get("url").toString();
                        Bitmap bitmap = downloadBitmap(url);
                        publishProgress(bitmap);
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }catch (OutOfMemoryError e){
                Toast.makeText(getApplicationContext(), "Out of Memory", Toast.LENGTH_SHORT).show();
            }
            return nextUrl;
        }

        private Bitmap downloadBitmap(String url) {
            final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
            final HttpGet httpGet = new HttpGet(url);
            try{
                HttpResponse response = client.execute(httpGet);
                final int statusCode = response .getStatusLine().getStatusCode();
                if(statusCode != HttpStatus.SC_OK){
                    Log.w("ImageDownloader", "Error " +statusCode + " while retrieving bitmap from " + url);
                    return null;
                }
                final HttpEntity entity = response.getEntity();
                if (entity != null){
                    InputStream inputStream = null;
                    try{
                        inputStream = entity.getContent();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        if (smallCounter == 0 || smallCounter == 3){
                            int width = 500;
                            int height = 500;
                            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                            smallCounter = 0;
                        }else{
                            int width = 320;
                            int height = 320;
                            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                        }
                        smallCounter++;
                        WeakReference<Bitmap> weakBitmap = new WeakReference<Bitmap>(bitmap);

                        return weakBitmap.get();
                    }finally {
                        if(inputStream != null){
                            inputStream.close();
                        }
                        entity.consumeContent();
                    }
                }
            }
            catch (Exception e){
                httpGet.abort();
                Log.w("ImageDownloader", "Error while retrieving bitmap from " +url);
            }finally {
                if (client != null){
                    client.close();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(final Bitmap... bitmap) {
            super.onProgressUpdate(bitmap);
            TableLayout tableInstagram = (TableLayout) findViewById(R.id.table_1_instagram);

            //Create ImageView for picture to be placed in and set onClickListener for it.
            ImageView imageView = new ImageView(MainActivity.this);
            imageViewReference = new WeakReference<ImageView>(imageView);
            imageViewReference.get().setImageBitmap(bitmap[0]);
            imageViewReference.get().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //when an image is clicked, pop up a dialog and put the image in it
                    final Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.image_layout);
                    dialog.setTitle("#Selfie");
                    ImageView imageView1 = (ImageView) findViewById(R.id.display_image);
                    imageView1.setLayoutParams(new TableRow.LayoutParams(700, 700));
                    imageView1.setImageBitmap(bitmap[0]);

                    //if dialog button is clicked, close the custom dialog
                    Button dialogButton = (Button) dialog.findViewById(R.id.ok_dialog_button);
                    dialogButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
            });

            //Format the imageView Object
            TableRow.LayoutParams imageParams = new TableRow.LayoutParams();
            imageParams.setMargins(10, 10, 10, 10);
            imageParams.gravity = Gravity.CENTER;
            imageViewReference.get().setLayoutParams(imageParams);

            //Format the tablerow and add the imageView to it
            TableRow tableRow = new TableRow(MainActivity.this);
            tableRow.addView(imageViewReference.get());
            tableRow.setBackgroundColor(Color.parseColor("#000000"));
            TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(getResources().
                    getDisplayMetrics().widthPixels, TableRow.LayoutParams.WRAP_CONTENT);
            tableRow.setLayoutParams(layoutParams);

            //Add the TableRow to the TableLayout
            tableInstagram.addView(tableRow);

        }

        @Override
        protected void onPostExecute(String nextUrl) {
            if (noProgressDialog && progressDialog != null){
                progressDialog.dismiss();
            }
            //Get the next url to download images from and decrement AsyncCounter
            next_url = nextUrl;
            asyncCounter--;
            Log.d("Async Decrement", " " +asyncCounter);

            super.onPostExecute(nextUrl);
        }

    }
    @Override
    public void onScrollChanged(ScrollViewExt scrollViewExt, int x, int y, int oldx, int oldy) {

        View view = (View) scrollViewExt.getChildAt(scrollViewExt.getChildCount() -1);
        int diff = (view.getBottom() - (scrollViewExt.getHeight() + scrollViewExt.getScrollY()));

        //if diff is zero, then the bottom has been reached and if AsyncCounter is 0, start new task
        if(diff == 0 && asyncCounter == 0){
            new LoadInstagramImages(false).execute(next_url);
        }
    }
}

