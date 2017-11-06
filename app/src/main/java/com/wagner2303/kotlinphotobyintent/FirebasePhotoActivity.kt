package com.wagner2303.kotlinphotobyintent

import android.app.Activity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.lang.Exception
import java.lang.ref.WeakReference

class FirebasePhotoActivity : AppCompatActivity() {

    private var originalFileName: String? = null
    private var fileUri: String? = null
    private var firebaseFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firebase_photo)

        originalFileName = intent.getStringExtra(MainActivity.IMAGE_FILE_KEY)
        fileUri = intent.getStringExtra(MediaStore.EXTRA_OUTPUT)
        if (originalFileName.isNullOrEmpty() || fileUri.isNullOrEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        val imageRef = FirebaseStorage.getInstance()
                .reference
                .child("originais/$originalFileName.jpg")

        firebaseFile = File.createTempFile("firebase_", ".jpg", cacheDir)

        val weakContext = WeakReference(this)
        imageRef.getFile(firebaseFile!!)
                .addOnProgressListener { weakContext.get()?.progressUpdated(it) }
                .addOnSuccessListener { weakContext.get()?.fileDownloaded(it) }
                .addOnFailureListener { weakContext.get()?.downloadFailure(it)}
    }

    private fun progressUpdated(snapshot: FileDownloadTask.TaskSnapshot?) {

    }

    private fun fileDownloaded(result: FileDownloadTask.TaskSnapshot) {
        firebaseFile?.let {
            it.copyTo(File(fileUri!!), overwrite = true)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

        private fun downloadFailure(exception: Exception) {
        throw exception
    }
}
