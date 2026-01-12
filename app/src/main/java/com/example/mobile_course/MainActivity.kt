package com.example.mobile_course

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import com.example.mobile_course.databinding.MainActivityBinding

class MainActivity : AppCompatActivity(R.layout.main_activity) {

//    private val Context.dataStore: DataStore<Ima> by preferencesDataStore(name = "set")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        debugging("onCreate")

        enableEdgeToEdge()

    }


}