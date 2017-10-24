package com.wagner2303.kotlinphotobyintent

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val PHOTO_FILE_SYSTEM = 212
        const val REQUEST_IMAGE_CAPTURE = 1
        const private val IMAGE_FILE_KEY = "image_file_key"
    }

    private var mCurrentPhotoPath: String? = null
    private var disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mCurrentPhotoPath = savedInstanceState?.getString(IMAGE_FILE_KEY, "")
    }

    override fun onStart() {
        super.onStart()
        if (disposables.isDisposed) {
            disposables = CompositeDisposable()
        }
        disposables.add(RxView.clicks(button)
                .subscribeBy { takePhoto() })
    }

    override fun onStop() {
        super.onStop()
        disposables.dispose()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(IMAGE_FILE_KEY, mCurrentPhotoPath ?: "")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE){
            if (resultCode == Activity.RESULT_OK) {
                val file = File(mCurrentPhotoPath)
                if (file.length() > 0) {
                    // TODO compress image
                } else {
                    Toast.makeText(this, "Arquivo corrompido, tente novamente", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            startPhotoIntent()
        } else {
            ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PHOTO_FILE_SYSTEM
            )
        }
    }

    private fun startPhotoIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val imageFile = createImageFile()
            if (imageFile != null) {
                val uri = FileProvider.getUriForFile(this,
                        "com.wagner2303.kotlinphotobyintent.fileprovider", imageFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val externalStorage = Environment.getExternalStorageDirectory()
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("COMPROVANTE_${timeStamp}_", ".jpg", storageDir)
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = imageFile.getAbsolutePath();
        return imageFile
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            when(requestCode){
                PHOTO_FILE_SYSTEM -> startPhotoIntent()
            }
        }
    }
}
