package com.hamzaerdas.a33_artbookkotlin

import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.hamzaerdas.a33_artbookkotlin.databinding.ActivityDetailsBinding
import com.hamzaerdas.a33_artbookkotlin.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.security.Permission
import java.util.jar.Manifest

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    // ActivityResultLauncher -> Galeriye gidip bir resim seçip gelindiğinde veya izin istendiğinde sonucu almak için kullanılır
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    var selectedBitmap : Bitmap? = null
    private lateinit var database: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if(info.equals("old")){
            binding.button.visibility = View.INVISIBLE

            val selectedId = intent.getIntExtra("id", 1)

            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))
            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artArtistName.setText(cursor.getString(artistNameIx))
                binding.artYear.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }

        }
        else if (info.equals("add")){
            binding.artNameText.setText("")
            binding.artArtistName.setText("")
            binding.artYear.setText("")
            binding.button.visibility = View.VISIBLE
            binding.imageView.setImageResource(R.drawable.selectimage)
        }
        else if (info.equals("delete")){

        }
    }

    fun save(view: View){
        val artName = binding.artNameText.text.toString()
        val artArtistName = binding.artArtistName.text.toString()
        val artYear = binding.artYear.text.toString()

        if(selectedBitmap != null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!, 300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            try {

                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

                val sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?,?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1, artName)
                statement.bindString(2, artArtistName)
                statement.bindString(3, artYear)
                statement.bindBlob(4, byteArray)
                statement.execute()

            } catch (e: Exception){
                e.printStackTrace()
            }

            val intent = Intent(this@DetailsActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    private fun makeSmallerBitmap(image: Bitmap, maximumSize: Int): Bitmap{
        var height = image.height
        var widht = image.width

        val bitmapRatio: Double =  widht.toDouble() / height.toDouble()
        if(bitmapRatio > 1){
            //landscape
            widht = maximumSize
            val scaledHeight = widht / bitmapRatio
            height = scaledHeight.toInt()
        }else {
            //portrait
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            widht = scaledWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image, widht, height, true)
    }

    fun selectImage(view: View){
        // API Level'ı kontrol eder ve izin istenilmesine gerek olup olmadığına karar verir.
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            // İzin verilmedi. Ne yapılacak

            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                // Eğer kullanıcı izin vermezse açıklama yaz.
                Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", View.OnClickListener {
                    // request permission
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }).show()
            } else{
                // request permission
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        } else {
            // İzin verildi. Galeriye git.
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
    }

    private fun registerLauncher(){

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->

            // Kullanıcı galeriye gidip bir şey seçti mi kontrolü
            if(result.resultCode == RESULT_OK){
                val intentFromResult = result.data

                // Gelen veri null kontrolü
                if(intentFromResult != null){
                    val imageData = intentFromResult.data

                    if(imageData != null){
                        try {

                            // Seçilen resmi bitmap e çevirip image view'a yansıtma
                            if (Build.VERSION.SDK_INT >= 28){
                                val source = ImageDecoder.createSource(this@DetailsActivity.contentResolver, imageData)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            } else{
                                selectedBitmap = MediaStore.Images.Media.getBitmap(this@DetailsActivity.contentResolver, imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }

                        } catch (e: Exception){
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->

            if(result){
                // Permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else{
                // Permission denied
                Toast.makeText(this@DetailsActivity, "Permission needed!!!", Toast.LENGTH_LONG).show()
            }

        }
    }
}