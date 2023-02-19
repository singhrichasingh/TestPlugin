package cordova.plugin.richa;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.Manifest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import android.provider.MediaStore;
import android.os.Environment;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import android.util.Base64;
import android.net.Uri;
import android.database.Cursor;

public class Richa extends CordovaPlugin {
    private int destType, encodingType;
    public static final int TAKE_PIC_SEC = 0;
    private static final String LOG_TAG = "CameraLauncher";
    private static final int CAMERA = 1; 
    protected final static String[] permissions = { Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private int numPics;
    private Uri imageUri;                   
    private String imageFilePath;
    private String applicationId;
    private static final int JPEG = 0; 
    private static final int PNG = 1; 
    private static final String JPEG_TYPE = "jpg";
    private static final String PNG_TYPE = "png";
    private static final String JPEG_EXTENSION = "." + JPEG_TYPE;
    private static final String PNG_EXTENSION = "." + PNG_TYPE;
   

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
     if (action.equals("start")) {
        encodingType = Integer.parseInt(args.getJSONObject(0).getString("encodingType"));
        destType = Integer.parseInt(args.getJSONObject(0).getString("destType"));
            this.callTakePicture(destType, encodingType);
            return true;
        }
       else  if (action.equals("init")) {
                this.init(callbackContext);
                return true;
            }
        return false;
    }
 
    public void init(CallbackContext callbackContext){
        LOG.d(LOG_TAG, "started");
        callbackContext.success("3 startgs");
    }

    public void callTakePicture(int returnType, int encodingType) {
        boolean saveAlbumPermission = PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                && PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean takePicturePermission = PermissionHelper.hasPermission(this, Manifest.permission.CAMERA);

        if (!takePicturePermission) {
            takePicturePermission = true;
            try {
                PackageManager packageManager = this.cordova.getActivity().getPackageManager();
                String[] permissionsInPackage = packageManager.getPackageInfo(this.cordova.getActivity().getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
                if (permissionsInPackage != null) {
                    for (String permission : permissionsInPackage) {
                        if (permission.equals(Manifest.permission.CAMERA)) {
                            takePicturePermission = false;
                            break;
                        }
                    }
                }
            } catch (NameNotFoundException e) {
            }
        }

        if (takePicturePermission && saveAlbumPermission) {
            takePicture(returnType, encodingType);
        } else if (saveAlbumPermission) {
            PermissionHelper.requestPermission(this, TAKE_PIC_SEC, Manifest.permission.CAMERA);
        } else if (takePicturePermission) {
            PermissionHelper.requestPermissions(this, TAKE_PIC_SEC,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
        } else {
            PermissionHelper.requestPermissions(this, TAKE_PIC_SEC, permissions);
        }
    }



    public void takePicture(int returnType, int encodingType) {
        this.numPics = queryImgDB(whichContentStore()).getCount();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = createCaptureFile(encodingType);
        this.imageFilePath = photo.getAbsolutePath();
        this.imageUri = FileProvider.getUriForFile(cordova.getActivity(),
                applicationId + ".cordova.plugin.camera.provider",
                photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (this.cordova != null) {
            PackageManager mPm = this.cordova.getActivity().getPackageManager();
            if(intent.resolveActivity(mPm) != null)
            {
                this.cordova.startActivityForResult((CordovaPlugin) this, intent, (CAMERA + 1) * 16 + returnType + 1);
            }
            else
            {
                LOG.d(LOG_TAG, "Error: You don't have a default camera.  Your device may not be CTS complaint.");
            }
        }}

        private File createCaptureFile(int encodingType) {
            return createCaptureFile(encodingType, "");
        }

        private File createCaptureFile(int encodingType, String fileName) {
            if (fileName.isEmpty()) {
                fileName = ".Pic";
            }
    
            if (encodingType == JPEG) {
                fileName = fileName + JPEG_EXTENSION;
            } else if (encodingType == PNG) {
                fileName = fileName + PNG_EXTENSION;
            } else {
                throw new IllegalArgumentException("Invalid Encoding Type: " + encodingType);
            }
    
            return new File(getTempDirectoryPath(), fileName);
        }
    

        private Uri whichContentStore() {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else {
                return MediaStore.Images.Media.INTERNAL_CONTENT_URI;
            }
        }

        private Cursor queryImgDB(Uri contentStore) {
            return this.cordova.getActivity().getContentResolver().query(
                    contentStore,
                    new String[]{MediaStore.Images.Media._ID},
                    null,
                    null,
                    null);
        }

        private String getTempDirectoryPath() {
            File cache = cordova.getActivity().getCacheDir();
            // Create the cache directory if it doesn't exist
            cache.mkdirs();
            return cache.getAbsolutePath();
        }
}
