package bitwalking.bitwalking.events;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 9/27/16.
 */
public class ManageImagesDownload {
    private static final String TAG = ManageImagesDownload.class.getSimpleName();

    private String _imagesFolderName = "default_folder";
    private File _imagesFolder;
    private Context _context;

    public ManageImagesDownload(Context context) {
        _context = context;
        _imagesFolder = createImagesFolder();
    }

    public ManageImagesDownload(Context context, String imagesFolderName) {
        _context = context;
        _imagesFolderName = imagesFolderName;
        _imagesFolder = createImagesFolder();
    }

    private void storeImage(String imageName, Bitmap image) {
        Logger.instance().Log(Logger.DEBUG, TAG, "store image: " + getImagePath(imageName));
        File imageFile = new File(getImagePath(imageName));

        try {
            FileOutputStream out = new FileOutputStream(imageFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteImage(String imageName) {
        File imageFile = new File(getImagePath(imageName));

        if (imageFile.exists()) {
            Logger.instance().Log(Logger.DEBUG, TAG, "delete image: " + getImagePath(imageName));
            imageFile.delete();
        }
    }

    /*
     * This method will return the banner out of downloaded banner and not out of the market list
     */
    public Bitmap getImage(String imageName, String imageUrl) {
        Bitmap image = null;

        if (imageExists(imageName)) {
            Logger.instance().Log(Logger.DEBUG, TAG, "load image: " + getImagePath(imageName));
            File imageFile = new File(getImagePath(imageName));

            try {
                image = BitmapFactory.decodeStream(new FileInputStream(imageFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            image = downloadAndStoreImage(imageName, imageUrl);
        }

        return image;
    }

    public boolean imageExists(String imageName) {
        File imageFile = new File(getImagePath(imageName));

        return imageFile.exists();
    }

    private String getImagePath(String imageName) {
        return _imagesFolder.getPath() + "/" + imageName;
    }

    private Bitmap downloadAndStoreImage(String imageName, String imageUrl) {
        Logger.instance().Log(Logger.DEBUG, TAG, "download image: " + getImagePath(imageName));
        Bitmap image = null;
        try {
            image = BitmapFactory.decodeStream(new URL(imageUrl).openConnection().getInputStream());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (null != image)
            storeImage(imageName, image);

        return image;
    }

    private File createImagesFolder() {
        File myDir = new File(_context.getFilesDir(), _imagesFolderName);
        if (!myDir.exists())
            myDir.mkdirs();

        return myDir;
    }

}
