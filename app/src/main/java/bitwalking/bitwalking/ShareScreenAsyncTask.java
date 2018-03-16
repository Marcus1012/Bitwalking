package bitwalking.bitwalking;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import bitwalking.bitwalking.activityes.MainActivity;

/**
 * Created by Marcus on 11/22/15.
 */
public class ShareScreenAsyncTask extends AsyncTask<String, Void, Void> {

    static final String _SHARE_IMAGES_FOLDER = "shares";
    Activity _activity;
    Context _context;
    Bitmap _screen;

    public ShareScreenAsyncTask(Activity activity, Bitmap screen) {
        _activity = activity;
        _context = _activity.getBaseContext();
        _screen = screen;
    }

    private void shareSavedImage(String title, String text) {
        File imagePath = new File(_context.getFilesDir(), _SHARE_IMAGES_FOLDER);
        File newFile = new File(imagePath, "image.png");
        Uri contentUri = FileProvider.getUriForFile(_context.getApplicationContext(), "bitwalking.bitwalking.fileprovider", newFile);

        if (contentUri != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setType(_context.getContentResolver().getType(contentUri));
            // Share subject
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
            // Share text
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
            // Share image
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.removeExtra(android.content.Intent.EXTRA_EMAIL);
            _activity.startActivityForResult(Intent.createChooser(shareIntent, "Share your day"), MainActivity.SHARE_TODAY_REQUEST);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(String... args) {
        if (saveImageInternal(Bitmap.createScaledBitmap(_screen, 768, 1366, false))) {
            shareSavedImage(args[0], args[1]);
        }

        return null;
    }

    private boolean saveImageInternal(Bitmap image) {
        boolean saved = false;

        // save bitmap to cache directory
        try {
            File cachePath = new File(_context.getFilesDir(), _SHARE_IMAGES_FOLDER);
            cachePath.mkdirs(); // don't forget to make the directory
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            saved = true;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return saved;
    }
}
