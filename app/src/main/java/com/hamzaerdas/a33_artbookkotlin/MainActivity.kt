package com.hamzaerdas.a33_artbookkotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.hamzaerdas.a33_artbookkotlin.databinding.ActivityDetailsBinding
import com.hamzaerdas.a33_artbookkotlin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var artList: ArrayList<Art>
    private lateinit var artAdapter: ArtAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        artList = ArrayList<Art>()

        artAdapter = ArtAdapter(artList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        binding.recyclerView.adapter = artAdapter

        try {

            val database = openOrCreateDatabase("Arts", MODE_PRIVATE, null)

            val cursor = database.rawQuery("SELECT * FROM arts", null)
            val idIx = cursor.getColumnIndex("id")
            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")

            while (cursor.moveToNext()){
                val id = cursor.getInt(idIx)
                val name = cursor.getString(artNameIx)
                val artistName = cursor.getString(artistNameIx)

                val art = Art(id, name, artistName)
                artList.add(art)

                println("İd: ${id}, Name: ${name}, Artist Name: ${artistName}")
            }

            artAdapter.notifyDataSetChanged()

            cursor.close()

        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    // Menü ile ilgili işlemler
    // Bağlama işlemi
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.art_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // Menüye tıklanınca ne olacak
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.add_art_item){
            val intent = Intent(this@MainActivity, DetailsActivity::class.java)
            intent.putExtra("info", "add")
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}