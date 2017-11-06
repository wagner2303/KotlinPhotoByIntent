package com.wagner2303.kotlinphotobyintent

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.test.espresso.idling.CountingIdlingResource
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.jakewharton.rxbinding2.widget.itemClicks
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
        const val IMAGE_FILE_KEY = "image_file_key"
        const private val IMAGE_REQUESTED_KEY = "image_requested_key"
        const private val FILE_INDEX_KEY = "file_index_key"
        private const val MIN_DIMEN = 1080
        private const val COMPRESSION = 70

        private val testFiles = listOf("04", "05", "06", "08", "09", "12")
    }

    private val mStorageRef : StorageReference by lazy { FirebaseStorage.getInstance().reference }
    private var mCurrentPhotoPath: String? = null
    private var mPhotoIndex: Int = 0
    private var mFileRequested: String? = null

    private var disposables = CompositeDisposable()
    var idleResource : CountingIdlingResource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("DEVICE_DATA", Build::class.java.fields.map { "Build.${it.name} = ${it.get(it.name)}"}.joinToString("\n"))

        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, testFiles)


        savedInstanceState?.let {
            mCurrentPhotoPath = it.getString(IMAGE_FILE_KEY, "")
            mPhotoIndex = it.getInt(FILE_INDEX_KEY, 0)
            mFileRequested = it.getString(IMAGE_REQUESTED_KEY, "")
        }
    }

    override fun onStart() {
        super.onStart()
        if (disposables.isDisposed) {
            disposables = CompositeDisposable()
        }
//        disposables.add(RxView.clicks(button)
//                .subscribeBy { takePhoto() })
        disposables.add(listView.itemClicks().subscribeBy {
            takePhoto(testFiles[it])
        })
    }

    override fun onStop() {
        super.onStop()
        disposables.dispose()
    }


    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(IMAGE_FILE_KEY, mCurrentPhotoPath ?: "")
        outState?.putString(IMAGE_REQUESTED_KEY, mFileRequested ?: "")
        outState?.putInt(FILE_INDEX_KEY, mPhotoIndex)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE){
            if (resultCode == Activity.RESULT_OK) {
                val file = File(mCurrentPhotoPath)
                if (file.length() > 0) {
                    System.gc()
                    val size = file.length()
                    val filesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val target100 = File(filesDir, "100.jpg")
                    file.copyTo(target100, overwrite = true)

                    val scaledOptions = target100.getScaledOptions(MIN_DIMEN)
                    val fileName = "compressed.jpg"
                    val target = target100.compress(COMPRESSION, File(filesDir, fileName), scaledOptions)
                    Log.d("COMPRESS", "$fileName\tFile size:\t${target.length()}\tRate: $COMPRESSION%")

                    // TODO compress image
//                    Glide.with(this)
//                            .load(target)
//                            .apply(RequestOptions.fitCenterTransform())
//                            .into(image)
                    System.gc()

                    // Upload Image to Firebase
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(target100.absolutePath, options)

                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

                    val imageRef = mStorageRef.child("images" +
                            "/${Build.MANUFACTURER} ${Build.MODEL}" +
                            "/$mFileRequested" +
                            "_${options.scaleTo(MIN_DIMEN)}" +
                            "_$COMPRESSION" +
                            "_${timeStamp}.jpg")
                    mPhotoIndex++
                    imageRef.putFile(Uri.fromFile(target)).addOnSuccessListener {
                        Toast.makeText(this, "Upload $mFileRequested concluído", Toast.LENGTH_SHORT).show()
                        idleResource?.decrement()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Upload $mFileRequested falhou", Toast.LENGTH_SHORT).show()
                        idleResource?.decrement()
                    }
                } else {
                    Toast.makeText(this, "Arquivo corrompido, tente novamente", Toast.LENGTH_SHORT).show()
                    idleResource?.decrement()
                }
            } else {
                Toast.makeText(this, "Erro ao baixar arquivo, tente novamente", Toast.LENGTH_SHORT).show()
                idleResource?.decrement()
            }
        }
    }

    private fun takePhoto(fileRequested: String) {
        mFileRequested = fileRequested
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
        val imageFile = createImageFile()
        if (imageFile != null) {
            val photoIntent = Intent(this, FirebasePhotoActivity::class.java)
            val uri = FileProvider.getUriForFile(this, "com.wagner2303.kotlinphotobyintent.fileprovider", imageFile)
            photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageFile.absolutePath)
            photoIntent.putExtra(IMAGE_FILE_KEY, mFileRequested)
            idleResource?.increment()
            startActivityForResult(photoIntent, REQUEST_IMAGE_CAPTURE)
        }
//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (takePictureIntent.resolveActivity(packageManager) != null) {
//            val imageFile = createImageFile()
//            if (imageFile != null) {
//                val uri = FileProvider.getUriForFile(this,
//                        "com.wagner2303.kotlinphotobyintent.fileprovider", imageFile)
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
//                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//            }
//        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val externalStorage = Environment.getExternalStorageDirectory()
        val storageDir = cacheDir // getExternalFilesDir(Environment.DIRECTORY_PICTURES) //externalCacheDir
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
        } else {
            idleResource?.decrement()
        }
    }
}

private fun BitmapFactory.Options.scaleTo(dimenMin: Int): Int {
    val dimen = Math.min(outWidth, outHeight)
    return largestPowerOf2(dimen.toDouble()/dimenMin.toDouble())
}

fun largestPowerOf2(i: Double) = Math.pow(2.0, Math.floor(Math.log(i)/Math.log(2.0))).toInt()

private fun File.compress(compression: Int, outputFile: File, scaledOptions: BitmapFactory.Options? = null): File {
    val bitmap = BitmapFactory.decodeFile(absolutePath, scaledOptions)
    outputFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, compression, it) }
    return outputFile
}

private fun File.getScaledOptions(dimen: Int): BitmapFactory.Options {
    val options = BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(absolutePath, options);
    options.inSampleSize = options.scaleTo(dimen);
    options.inJustDecodeBounds = false;
    return options
}

