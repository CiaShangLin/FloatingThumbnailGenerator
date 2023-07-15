package com.shang.floatingthumbnailgenerator


import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shang.floatingthumbnailgenerator.databinding.ActivityMainBinding
import com.yalantis.ucrop.UCrop
import java.io.File

/** Intent action for stopwatch controls from Picture-in-Picture mode.  */
private const val ACTION_STOPWATCH_CONTROL = "stopwatch_control"

/** Intent extra for stopwatch controls from Picture-in-Picture mode.  */
private const val EXTRA_CONTROL_TYPE = "control_type"
private const val CONTROL_TYPE_CLEAR = 1
private const val CONTROL_TYPE_START_OR_PAUSE = 2

private const val REQUEST_CLEAR = 3
private const val REQUEST_START_OR_PAUSE = 4

/**
 * Demonstrates usage of Picture-in-Picture mode on phones and tablets.
 */
class MainActivity : AppCompatActivity() {
    private val _PickImageRequestCode = 200

    private lateinit var binding: ActivityMainBinding
    private var mNumerator = 16
    private var mDenominator = 9
    private var mTempImage: Uri? = null

    private val broadcastReceiver = object : BroadcastReceiver() {

        // Called when an item is clicked.
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action != ACTION_STOPWATCH_CONTROL) {
                return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageView.setOnClickListener {
            pickImage()
        }
        binding.pip.setOnClickListener {
            enterPictureInPictureMode(updatePictureInPictureParams())
        }
        binding.her.setOnClickListener {
            mNumerator = 16
            mDenominator = 9
            val set=ConstraintSet()
            set.clone(binding.root)
            set.setDimensionRatio(R.id.imageView,"${mNumerator}:${mDenominator}")
            set.applyTo(binding.root)
        }
        binding.ver.setOnClickListener {
            mNumerator = 9
            mDenominator = 16
            val set=ConstraintSet()
            set.clone(binding.root)
            set.setDimensionRatio(R.id.imageView,"${mNumerator}:${mDenominator}")
            set.applyTo(binding.root)
        }
        binding.et.setOnClickListener {
            val file = File(filesDir.absolutePath, "new.png")
            if (!file.exists()) {
                file.createNewFile()
            }
            UCrop.of(mTempImage!!, Uri.fromFile(file))
                .withAspectRatio(mNumerator.toFloat(), mDenominator.toFloat())
                //.withMaxResultSize(1080, 1920)
                .start(this);
        }

        registerReceiver(broadcastReceiver, IntentFilter(ACTION_STOPWATCH_CONTROL))
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.ver.visibility = View.INVISIBLE
            binding.her.visibility = View.INVISIBLE
            binding.et.visibility = View.INVISIBLE
            binding.pip.visibility = View.INVISIBLE
        } else {
            binding.ver.visibility = View.VISIBLE
            binding.her.visibility = View.VISIBLE
            binding.et.visibility = View.VISIBLE
            binding.pip.visibility = View.VISIBLE
        }
    }

    private fun updatePictureInPictureParams(): PictureInPictureParams {
        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(mNumerator, mDenominator))
                .setAutoEnterEnabled(true)
                .setSeamlessResizeEnabled(false)
                .build()
        } else {
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(mNumerator, mDenominator))
                .build()
        }

        setPictureInPictureParams(params)
        return params
    }


    private fun createRemoteAction(
        @DrawableRes iconResId: Int,
        @StringRes titleResId: Int,
        requestCode: Int,
        controlType: Int
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(this, iconResId),
            getString(titleResId),
            getString(titleResId),
            PendingIntent.getBroadcast(
                this,
                requestCode,
                Intent(ACTION_STOPWATCH_CONTROL)
                    .putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun pickImage() {
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, "Select image"), _PickImageRequestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == _PickImageRequestCode) {
            binding.imageView.setImageURI(data?.data)
            mTempImage = data?.data
        }

        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            var resultUri = UCrop.getOutput(data!!)
            binding.imageView.setImageURI(resultUri)
        }
    }

}
