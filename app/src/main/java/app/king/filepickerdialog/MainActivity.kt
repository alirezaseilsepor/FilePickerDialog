package app.king.filepickerdialog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.king.filePicker.FilePickerDialog
import com.canhub.cropper.CropImageOptions
import net.time4j.android.ApplicationStarter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ApplicationStarter.initialize(this, false)

        FilePickerDialog.Builder()
            //.setMultiSelect(true,3)
            .setEnableCamera(false)
            .setCompress(true, 100)
            .setEnableCrop(true, CropImageOptions())
            .setSingleSelectListener {
                //  viewModel.uploadImageProfile(File(it.path))
            }
            .build()
            .show(supportFragmentManager, null)

    }
}