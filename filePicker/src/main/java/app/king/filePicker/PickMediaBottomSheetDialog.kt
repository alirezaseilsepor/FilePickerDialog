package app.king.filePicker

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.lifecycleScope
import app.king.filePicker.databinding.DialogMediaPickBinding
import app.king.mylibrary.date.GeneralCalendar
import app.king.mylibrary.ktx.Click
import app.king.mylibrary.ktx.SimpleClick
import app.king.mylibrary.ktx.safeDismiss
import app.king.mylibrary.ktx.setOnSafeClickListener
import coil.load
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.permissionx.guolindev.PermissionX
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.shouheng.compress.Compress
import me.shouheng.compress.concrete
import java.io.*
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.math.roundToInt

class PickMediaBottomSheetDialog : BaseBottomSheetDialogFragment<DialogMediaPickBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> DialogMediaPickBinding
        get() = DialogMediaPickBinding::inflate

    private val resultMediaList = arrayListOf<ResultMedia>()
    private val resultFolderList = arrayListOf<String>()
    private var selectedListResultMedia = arrayListOf<ResultMedia>()
    private var cameraFileCompat: FileCompat? = null
    var isMultiSelect = false
    var onSelectFileListener: Click<ArrayList<ResultMedia>>? = null
    var onCancelListener: SimpleClick? = null
    var isCompress = false
    var compressQuality = 70
    var isEnableCrop = false
    var cropImageOptions: CropImageOptions = CropImageOptions()

    private val takeImageResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                cameraFileCompat?.let { fileCompat ->
                    if (isEnableCrop) {
                        cropImage.launch(CropImageContractOptions(fileCompat.uri, cropImageOptions))
                    } else {
                        if (isCompress) {
                            compressImage(fileCompat)
                        } else
                            onSelectFileListener?.invoke(arrayListOf(createResultMedia(fileCompat)))
                    }
                }
            } else {
                onCancelListener?.invoke()
            }
        }

    private val cropImage = registerForActivityResult(CustomCropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            val filePath = result.getUriFilePath(requireContext())!!
            val file = File(filePath)
            val fileCompat = FileCompat(file, uriContent!!)
            if (isCompress) {
                compressImage(fileCompat)
            } else
                onSelectFileListener?.invoke(arrayListOf(createResultMedia(fileCompat)))
        } else {
            onCancelListener?.invoke()
            // val exception = result.error
        }
    }

    override fun setup() {
        requestStoragePermission { isGrant ->
            if (isGrant) {
                if (resultMediaList.isEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        getImages()
                        withContext(Dispatchers.Main) {
                            showMedia()
                        }
                    }
                }
            } else
                safeDismiss()
        }


        binding.btnConfirm.setOnSafeClickListener {
            if (isEnableCrop && !isMultiSelect) {
                val uri = Uri.parse(selectedListResultMedia.first().uriPath)
                cropImage.launch(CropImageContractOptions(uri, cropImageOptions))
            } else
                onSelectFileListener?.invoke(selectedListResultMedia)
        }
    }

    private fun getImages() {
        val imageProjection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val maxOldOriginalDate = GeneralCalendar.getInstance().addYears(-7).getGeorgianDate().time
        val currentDateTime = GeneralCalendar.getInstance().getGeorgianDate().time
        val orderBy = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val query = requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            null,
            null,
            orderBy
        )
        query.use { cursor ->
            cursor?.let {
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val folderColumn =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val size = it.getStringOrNull(sizeColumn)
                    val dateTaken = it.getLongOrNull(dateTakenColumn)
                    val dateAdded = it.getLong(dateAddColumn)
                    val path = it.getString(pathColumn)
                    val folder = it.getString(folderColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    var originalDate = dateTaken ?: dateAdded

                    if (originalDate < maxOldOriginalDate)
                        originalDate = currentDateTime

                    val resultMedia = ResultMedia(
                        id,
                        name,
                        size,
                        originalDate,
                        path,
                        contentUri.toString(),
                        folder,
                    )
                    resultMediaList.add(resultMedia)
                }
                cursor.close()
            }
        }
    }

    private fun showMedia() {
        val adapter = PickMediaAdapter()
        adapter.selectedListResultMedia = selectedListResultMedia
        adapter.onClickCameraListener = {
            openCamera()
        }
        adapter.onSelectFileListener = { resultMedia, isSelect ->
            if (isMultiSelect)
                selectedListResultMedia.remove(resultMedia)
            else
                selectedListResultMedia.clear()
            if (isSelect)
                selectedListResultMedia.add(resultMedia)
            adapter.selectedListResultMedia = selectedListResultMedia
            binding.btnConfirm.isEnabled = selectedListResultMedia.isNotEmpty()
        }
        adapter.onSelectMediaListener = { resultMedia, targeImageView ->
            StfalconImageViewer.Builder(context, arrayListOf(resultMedia)) { view, image ->
                view.load(File(image.path))
            }
                .withTransitionFrom(targeImageView)
                .show()
        }
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = adapter
        adapter.submitList(getListMediaAdapter(resultMediaList))

        resultFolderList.clear()
        resultFolderList.add(getString(R.string.recent))
        resultFolderList.addAll(resultMediaList.map { it.folderName }.distinct())

        binding.spinnerFolder.adapter =
            SpinnerMediaAdapter(
                requireContext(),
                R.layout.item_spinner,
                resultFolderList
            ) { folder ->
                binding.spinnerFolder.apply {
                    setSelection(folder.second, false)
                    adapter.submitList(null)
                    runCatching {
                        val method: Method =
                            Spinner::class.java.getDeclaredMethod("onDetachedFromWindow")
                        method.isAccessible = true
                        method.invoke(this)
                    }
                }
                if (folder.second == 0) {
                    adapter.submitList(getListMediaAdapter(resultMediaList))
                    return@SpinnerMediaAdapter
                }

                if (folder.first.isNotEmpty()) {
                    adapter.submitList(getListMediaAdapter(resultMediaList.filter { it.folderName == folder.first }))
                }
            }
    }

    private fun openCamera() {
        val cameraFileName = UUID.randomUUID().toString()
        val cameraFile =
            File("${getImageFolder(requireContext().applicationContext)}${cameraFileName}.png")
        val cameraUri = getUriFromFile(cameraFile, requireContext().applicationContext)
        cameraFileCompat = FileCompat(cameraFile, cameraUri)
        takeImageResult.launch(cameraUri)
    }

    private fun getListMediaAdapter(list: List<ResultMedia>): List<ResultMedia> {
        val result = arrayListOf<ResultMedia>()
        result.add(ResultMedia(-1, "", "0", 0, "", "", ""))
        result.addAll(list)
        return result
    }

    private fun getFileName(filePath: String): String {
        return filePath.substringAfterLast("/").substringBeforeLast(".")
    }

    private fun createResultMedia(
        fileCompat: FileCompat,
        folderName: String = "",
        date: Long = System.currentTimeMillis(),
    ): ResultMedia {
        return ResultMedia(
            System.nanoTime(),
            getFileName(fileCompat.file.path),
            fileCompat.file.length().toString(),
            date,
            fileCompat.file.path,
            fileCompat.uri.toString(),
            folderName,
        )
    }

    private fun compressImage(fileCompat: FileCompat) {
        lifecycleScope.launch(Dispatchers.IO) {
            val compressFile = Compress.with(requireContext(), fileCompat.file)
                .setQuality(compressQuality)
                .setFormat(Bitmap.CompressFormat.PNG)
                .concrete {
                    withIgnoreIfSmaller(true)
                }
                .get(Dispatchers.IO)
            val uri = getUriFromFile(compressFile, requireContext())
            val compressFileCompat = FileCompat(compressFile, uri)
            Log.e("compressor", "image compress " +
                    ((compressFile.length().toFloat() / fileCompat.file.length()
                        .toFloat()) * 100).roundToInt().toString() + "%")
            withContext(Dispatchers.Main) {
                onSelectFileListener?.invoke(
                    arrayListOf(createResultMedia(compressFileCompat)))
            }
        }
    }

    private fun requestStoragePermission(action: (Boolean) -> Unit) {
        val permissionX =
            PermissionX.init(this)!!.permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissionX
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    getString(R.string.permission_request_title),
                    getString(R.string.ok),
                    getString(R.string.cancel)
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    getString(R.string.permission_forward_setting),
                    getString(R.string.ok),
                    getString(R.string.cancel)
                )
            }
        permissionX
            .request { allGranted, _, _ ->
                action.invoke(allGranted)
            }
    }

    private fun getImageFolder(context: Context): String {
        val folder = File(context.getExternalFilesDir(null), "/IMAGE")
        if (folder.exists().not()) {
            folder.mkdir()
        }
        return folder.path + "/"
    }


    private fun getUriFromFile(file: File, context: Context): Uri {
        val authority = "app.king.filepickerdialog" + ".provider"
        try {
            Log.i("AIC", "Try get URI for scope storage - content://")
            return FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            try {
                Log.e("AIC", "${e.message}")
                Log.w(
                    "AIC",
                    "ANR Risk -- Copying the file the location cache to avoid 'external-files-path' bug for N+ devices",
                )
                // Note: Periodically clear this cache
                val cacheFolder = File(context.cacheDir, "CROP_LIB_CACHE")
                val cacheLocation = File(cacheFolder, file.name)
                var input: InputStream? = null
                var output: OutputStream? = null
                try {
                    input = FileInputStream(file)
                    output = FileOutputStream(cacheLocation) // appending output stream
                    input.copyTo(output)
                    Log.i(
                        "AIC",
                        "Completed Android N+ file copy. Attempting to return the cached file",
                    )
                    return FileProvider.getUriForFile(context, authority, cacheLocation)
                } catch (e: Exception) {
                    Log.e("AIC", "${e.message}")
                    Log.i("AIC", "Trying to provide URI manually")
                    val path = "content://$authority/files/my_images/"

                    if (Build.VERSION.SDK_INT >= 26) {
                        Files.createDirectories(Paths.get(path))
                    } else {
                        val directory = File(path)
                        if (!directory.exists()) directory.mkdirs()
                    }

                    return Uri.parse("$path${file.name}")
                } finally {
                    input?.close()
                    output?.close()
                }
            } catch (e: Exception) {
                Log.e("AIC", "${e.message}")

                if (Build.VERSION.SDK_INT < 29) {
                    val cacheDir = context.externalCacheDir
                    cacheDir?.let {
                        try {
                            Log.i(
                                "AIC",
                                "Use External storage, do not work for OS 29 and above",
                            )
                            return Uri.fromFile(File(cacheDir.path, file.absolutePath))
                        } catch (e: Exception) {
                            Log.e("AIC", "${e.message}")
                        }
                    }
                }
                // If nothing else work we try
                Log.i("AIC", "Try get URI using file://")
                return Uri.fromFile(file)
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelListener?.invoke()
    }
}




