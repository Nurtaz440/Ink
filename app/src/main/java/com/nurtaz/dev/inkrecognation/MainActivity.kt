package com.nurtaz.dev.inkrecognation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erlendhaartveit.gooeymenu2.GooeyMenu2
import com.nurtaz.dev.inkrecognation.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(),StrokeManager.DownLoadedModelsChangedListener, GooeyMenu2.GooeyMenuInterface {
    private var _binding : ActivityMainBinding? = null
    val binding get() = _binding!!

    val strokeManager = StrokeManager()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)){v,insets->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(systemBars.left,systemBars.top,systemBars.right,systemBars.bottom)
            insets
        }

//        binding.btnDownload.setOnClickListener {
//            strokeManager.download()
//        }
//        binding.btnRecognize.setOnClickListener {
//            strokeManager.recognize()
//        }
//        binding.btnClear.setOnClickListener {
//            strokeManager.reset()
//            binding.drawingView.clear()
//        }
//        binding.btnDelete.setOnClickListener {
//            strokeManager.deleteActiveModel()
//        }
        binding.drawingView.setStrokeManager(strokeManager)
        binding.statusTextView.setStrokeManager(strokeManager)
        strokeManager.setStatusChangedListener(binding.statusTextView)
        strokeManager.setContentChangedListener(binding.drawingView)
        strokeManager.setDownLoadedModelsChangedListener(this@MainActivity)

//        strokeManager.reset()

        strokeManager.setActiveModel("zxx-Zsye-x-emoji")//en, zxx-Zsye-x-emoji, zxx-Zsym-x-autodraw,zxx-Zsym-x-shapes
     //   strokeManager.setActiveModel("uz-Latn")//en, zxx-Zsye-x-emoji, zxx-Zsym-x-autodraw,zxx-Zsym-x-shapes
      //  strokeManager.setActiveModel("en")//en, zxx-Zsye-x-emoji, zxx-Zsym-x-autodraw,zxx-Zsym-x-shapes
        //strokeManager.setActiveModel("zxx-Zsym-x-autodraw")//en, zxx-Zsye-x-emoji, zxx-Zsym-x-autodraw,zxx-Zsym-x-shapes
      //  strokeManager.setActiveModel("zxx-Zsym-x-shapes")//zxx-Zsym-x-shapes
        binding.gooeyMenu.setOnMenuListener(this@MainActivity)
    }


    override fun onDownLoadedModelsChanged(downloadedLanguageTags: Set<String>) {

    }

    override fun menuOpen() {

    }

    override fun menuClose() {

    }

    override fun menuItemClicked(menuNumber: Int) {
        when (menuNumber) {
            1 -> {
                strokeManager.reset()
                binding.drawingView.clear()

            }

            2 -> {
                strokeManager.download()
            }
        }
    }
}