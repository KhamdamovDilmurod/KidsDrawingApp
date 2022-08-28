package khamdamov.dilmurod.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import khamdamov.dilmurod.kidsdrawingapp.databinding.ActivityMainBinding
import khamdamov.dilmurod.kidsdrawingapp.databinding.DialogBrushSizeBinding
import khamdamov.dilmurod.kidsdrawingapp.databinding.DialogCustomProgressBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var mImageButtonCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mImageButtonCurrentPaint = binding.llPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        binding.ibBrush.setOnClickListener {
            brushSizerChooserDialog()
        }

        binding.ibUndo.setOnClickListener {
            binding.drawingView.onClickUndo()
        }

        binding.ibGallery.setOnClickListener {
//
//            ImagePicker.with(this)
//                .crop()	    			//Crop image(Optional), Check Customization for more option
//                .compress(1024)			//Final image size will be less than 1 MB(Optional)
//                .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
//                .start()
            if (isReadStorageAllowed()) {

                // This is for selecting the image from local store or let say from Gallery/Photos.
                val pickPhoto = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(pickPhoto, GALLERY)
            } else {

                //If the app don't have storage access permission we will ask for it.
                requestStoragePermission()
            }

        }

        binding.ibSave.setOnClickListener {
            if(isReadStorageAllowed()){
                BitmapAsyncTask(getBitmapFromView(binding.flDrawingViewContainer)).execute()
            } else {
                requestStoragePermission()
            }
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == Activity.RESULT_OK) {
//            //Image Uri will not be null for RESULT_OK
//            val uri: Uri = data?.data!!
//
//            // Use Uri object instead of File to avoid storage permissions
//            binding.ivBackground.visibility = View.VISIBLE
//            binding.ivBackground.setImageURI(uri)
//        } else if (resultCode == ImagePicker.RESULT_ERROR) {
//            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun brushSizerChooserDialog() {
        val brushDialog = Dialog(this)

        val view = DialogBrushSizeBinding.inflate(layoutInflater)
        brushDialog.setContentView(view.root)
        brushDialog.setTitle("Brush size: ")

        view.ibSmallBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(5f)
            brushDialog.dismiss()
        }
        view.ibMediumBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(10f)
            brushDialog.dismiss()
        }
        view.ibLargeBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(20f)
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            binding.drawingView.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )
            mImageButtonCurrentPaint = view
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {

        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    private inner class BitmapAsyncTask(val mBitmap: Bitmap) : AsyncTask<Any, Void, String>() {

        private lateinit var mProgressDialog: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg p0: Any?): String {
            var result = ""
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(
                        externalCacheDir!!.absoluteFile.toString()
                                + File.separator + "KidsDrawingApp_"
                                + System.currentTimeMillis() / 1000 + ".png"
                    )
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if (!result!!.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully: $result",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong while saving the file.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result),null
            ) { path, uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/*"
                startActivity(Intent.createChooser(shareIntent,"Share"))
            }
        }
        private fun showProgressDialog(){
            mProgressDialog = Dialog(this@MainActivity)
            val view = DialogCustomProgressBinding.inflate(layoutInflater)
            mProgressDialog.setContentView(view.root)
            mProgressDialog.show()
        }

        private fun cancelProgressDialog(){
            mProgressDialog.dismiss()
        }
    }

    //--------------------------------------------------------

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //Checking the request code of our request
        if (requestCode == STORAGE_PERMISSION_CODE) {

            //If permission is granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@MainActivity,
                    "Permission granted now you can read the storage files.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                //Displaying another toast if permission is not granted
                Toast.makeText(
                    this@MainActivity,
                    "Oops you just denied the permission.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * This is override method here we get the selected image
     * based on the code what we have passed for selecting the image.
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {

                        // Here if the user selects the image from local storage make the image view visible.
                        // By Default we will make it VISIBILITY as GONE.
                        binding.ivBackground.visibility = View.VISIBLE

                        // Set the selected image to the backgroung view.
                        binding.ivBackground.setImageURI(data.data)
                    } else {
                        // If the selected image is not valid. Or not selected.
                        Toast.makeText(
                            this@MainActivity,
                            "Error in parsing the image or its corrupted.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun requestStoragePermission() {

        /**
         * Gets whether you should show UI with rationale for requesting a permission.
         * You should do this only if you do not have the permission and the context in
         * which the permission is requested does not clearly communicate to the user
         * what would be the benefit from granting this permission.
         * <p>
         * For example, if you write a camera app, requesting the camera permission
         * would be expected by the user and no rationale for why it is requested is
         * needed. If however, the app needs location for tagging photos then a non-tech
         * savvy user may wonder how location is related to taking photos. In this case
         * you may choose to show UI with rationale of requesting this permission.
         * </p>
         *
         * @param activity The target activity.
         * @param permission A permission your app wants to request.
         * @return Whether you can show permission rationale UI.
         *
         */
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            //If the user has denied the permission previously your code will come to this block
            //Here you can explain why you need this permission
            //Explain here why you need this permission
        }

        /**
         * Requests permissions to be granted to this application. These permissions
         * must be requested in your manifest, otherwise they will not be granted to your app.
         */

        //And finally ask for the permission
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )
    }

    private fun isReadStorageAllowed(): Boolean {
        //Getting the permission status
        // Here the checkSelfPermission is
        /**
         * Determine whether <em>you</em> have been granted a particular permission.
         *
         * @param permission The name of the permission being checked.
         *
         */
        val result = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )

        /**
         *
         * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
         * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
         *
         */
        //If permission is granted returning true and If permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
    }

    companion object {


        /**
         * Permission code that will be checked in the method onRequestPermissionsResult
         *
         * For more Detail visit : https://developer.android.com/training/permissions/requesting#kotlin
         */
        private const val STORAGE_PERMISSION_CODE = 1

        // This is to identify the selection of image from Gallery.
        private const val GALLERY = 2
    }

}