package app.king.filePicker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import app.king.filePicker.databinding.ActivityCropBinding
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.CropImageView.*

open class CropActivity :
    AppCompatActivity(),
    OnSetImageUriCompleteListener,
    OnCropImageCompleteListener {

    /**
     * Persist URI image to crop URI if specific permissions are required
     */
    private var cropImageUri: Uri? = null

    /**
     * the options that were set for the crop image
     */
    private lateinit var cropImageOptions: CropImageOptions

    /** The crop image view library widget used in the activity */
    private var cropImageView: CropImageView? = null
    private lateinit var binding: ActivityCropBinding
    private var latestTmpUri: Uri? = null
    private val pickImageGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            onPickImageResult(uri)
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setCropImageView(binding.cropImageView)
        val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
        cropImageUri = bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
        cropImageOptions =
            bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS) ?: CropImageOptions()

        if (savedInstanceState == null) {
            if (cropImageUri == null || cropImageUri == Uri.EMPTY) {
                when {
                    cropImageOptions.imageSourceIncludeGallery ->
                        pickImageGallery.launch("image/*")

                    else -> finish()
                }
            } else cropImageView?.setImageUriAsync(cropImageUri)
        } else {
            latestTmpUri = savedInstanceState.getString(BUNDLE_KEY_TMP_URI)?.toUri()
        }

        supportActionBar?.let {
            title = cropImageOptions.activityTitle.ifEmpty { "" }
            it.setDisplayHomeAsUpEnabled(true)
        }
        binding.btnCrop.setOnClickListener { cropImage() }
        binding.btnCancel.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

    }

    public override fun onStart() {
        super.onStart()
        cropImageView?.setOnSetImageUriCompleteListener(this)
        cropImageView?.setOnCropImageCompleteListener(this)
    }

    public override fun onStop() {
        super.onStop()
        cropImageView?.setOnSetImageUriCompleteListener(null)
        cropImageView?.setOnCropImageCompleteListener(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_KEY_TMP_URI, latestTmpUri.toString())
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResultCancel()
    }

    protected open fun onPickImageResult(resultUri: Uri?) {
        when (resultUri) {
            null -> setResultCancel()
            else -> {
                cropImageUri = resultUri
                cropImageView?.setImageUriAsync(cropImageUri)
            }
        }
    }

    override fun onSetImageUriComplete(view: CropImageView, uri: Uri, error: Exception?) {
        if (error == null) {
            if (cropImageOptions.initialCropWindowRectangle != null)
                cropImageView?.cropRect = cropImageOptions.initialCropWindowRectangle

            if (cropImageOptions.initialRotation > 0)
                cropImageView?.rotatedDegrees = cropImageOptions.initialRotation

            if (cropImageOptions.skipEditing) {
                cropImage()
            }
        } else setResult(null, error, 1)
    }

    override fun onCropImageComplete(view: CropImageView, result: CropResult) {
        setResult(result.uriContent, result.error, result.sampleSize)
    }

    /**
     * Execute crop image and save the result tou output uri.
     */
    open fun cropImage() {
        if (cropImageOptions.noOutputImage) setResult(null, null, 1)
        else cropImageView?.croppedImageAsync(
            saveCompressFormat = cropImageOptions.outputCompressFormat,
            saveCompressQuality = cropImageOptions.outputCompressQuality,
            reqWidth = cropImageOptions.outputRequestWidth,
            reqHeight = cropImageOptions.outputRequestHeight,
            options = cropImageOptions.outputRequestSizeOptions,
            customOutputUri = cropImageOptions.customOutputUri,
        )
    }

    /**
     * When extending this activity, please set your own ImageCropView
     */
    open fun setCropImageView(cropImageView: CropImageView) {
        this.cropImageView = cropImageView
    }

    /**
     * Result with cropped image data or error if failed.
     */
    open fun setResult(uri: Uri?, error: Exception?, sampleSize: Int) {
        setResult(
            error?.let { CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE } ?: RESULT_OK,
            getResultIntent(uri, error, sampleSize)
        )
        finish()
    }

    /**
     * Cancel of cropping activity.
     */
    open fun setResultCancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    /**
     * Get intent instance to be used for the result of this activity.
     */
    open fun getResultIntent(uri: Uri?, error: Exception?, sampleSize: Int): Intent {
        val result = CropImage.ActivityResult(
            cropImageView?.imageUri,
            uri,
            error,
            cropImageView?.cropPoints,
            cropImageView?.cropRect,
            cropImageView?.rotatedDegrees ?: 0,
            cropImageView?.wholeImageRect,
            sampleSize
        )
        val intent = Intent()
        intent.putExtras(getIntent())
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_RESULT, result)
        return intent
    }

    private companion object {
        const val BUNDLE_KEY_TMP_URI = "bundle_key_tmp_uri"
    }
}
