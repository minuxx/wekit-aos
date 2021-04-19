package com.coconutplace.wekit.ui.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.coconutplace.wekit.R
import com.coconutplace.wekit.data.remote.auth.listeners.ProfileListener
import com.coconutplace.wekit.databinding.ActivityProfileBinding
import com.coconutplace.wekit.ui.BaseActivity
import com.coconutplace.wekit.ui.certify_email.CertifyEmailActivity
import com.coconutplace.wekit.ui.edit_password.EditPasswordActivity
import com.coconutplace.wekit.ui.login.LoginActivity
import com.coconutplace.wekit.ui.main.MainActivity
import com.coconutplace.wekit.utils.*
import com.coconutplace.wekit.utils.GlobalConstant.Companion.PROFILE_URL
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class ProfileActivity : BaseActivity(), ProfileListener {
    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        viewModel.profileListener = this

        binding.profileRootLayout.setOnClickListener(this)
        binding.profileBackBtn.setOnClickListener(this)
        binding.profileEditTv.setOnClickListener(this)
        binding.profileProfileImgIv.setOnClickListener(this)
        binding.profileProfileImgIv.isClickable = false
        binding.profileEditCompleteBtn.setOnClickListener(this)
        binding.profileEditCompleteBtn.isClickable = false
        binding.profileNicknameEt.isFocusableInTouchMode = false
        binding.profileDeleteUserTv.setOnClickListener(this)
        binding.profileDeleteUserCompleteBtn.setOnClickListener(this)
        binding.profileDeleteUserCompleteBtn.isClickable = false
        binding.profileEditPasswordTv.setOnClickListener(this)
        binding.profileEditPasswordTv.isClickable = false

        if (intent.hasExtra(PROFILE_URL)) {
            Glide.with(this)
                .load(intent.getStringExtra(PROFILE_URL))
                .circleCrop()
                .placeholder(R.drawable.character_big_basic)
                .error(R.drawable.character_big_basic)
                .into(binding.profileProfileImgIv)
        }

        observeNickname()
        observeProfileUrlFromFirebase()
    }

    override fun onRestart() {
        super.onRestart()
        binding.profileEditPasswordTv.isClickable = true
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        when (v) {
            binding.profileBackBtn -> finish()
            binding.profileRootLayout -> hideKeyboard()

            binding.profileEditTv -> convertEditMode()
            binding.profileProfileImgIv -> pickImageFromGallery()
            binding.profileEditPasswordTv -> startCertifyEmailActivity()
            binding.profileEditCompleteBtn -> patchProfile()

            binding.profileDeleteUserTv -> convertDeleteUserMode()
            binding.profileDeleteUserCompleteBtn -> deleteUser()
        }
    }

    private fun hideKeyboard() {
        binding.profileRootLayout.hideKeyboard()
        binding.profileNicknameEt.clearFocus()
        binding.profilePwEt.clearFocus()
    }

    private fun patchProfile() {
        if (viewModel.mFlagEdit) {
            if (viewModel.profileUrl.value == null || viewModel.profileUrlFromFirebase.value!!.isNotEmpty()) {
                viewModel.patchProfile()
            } else if(viewModel.profileUrlFromFirebase.value!!.isEmpty()){
                viewModel.uploadToFirebase()
            }
        }
    }

    private fun deleteUser(){
        if(viewModel.mFlagDeleteUser){
            viewModel.deleteUser()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun convertEditMode() {
        if (viewModel.mFlagEdit) {
            viewModel.mFlagEdit = false
            binding.profileEditTv.text = getString(R.string.profile_edit)
            binding.profileProfileImgEditIv.visibility = View.INVISIBLE
            binding.profileEditCompleteBtn.visibility = View.INVISIBLE
            binding.profileProfileImgIv.isClickable = false
            binding.profileEditPasswordTv.isClickable = false

            binding.profileEditPasswordTv.background =
                getDrawable(R.drawable.bg_edit_password_button_inactive)
            binding.profileEditPasswordTv.setTextColor(getColor(R.color.profile_edit_password_btn_border_inactive))
            binding.profileNicknameEt.isFocusableInTouchMode = false
            binding.profileNicknameEt.clearFocus()

            binding.profileDeleteUserTv.isClickable = true
        } else {
            viewModel.mFlagEdit = true
            binding.profileEditTv.text = getString(R.string.profile_cancel)
            binding.profileProfileImgEditIv.visibility = View.VISIBLE
            binding.profileEditCompleteBtn.visibility = View.VISIBLE
            binding.profileProfileImgIv.isClickable = true
            binding.profileEditCompleteBtn.isClickable = true
            binding.profileEditPasswordTv.isClickable = true

            binding.profileEditPasswordTv.background =
                getDrawable(R.drawable.bg_edit_password_button_active)
            binding.profileEditPasswordTv.setTextColor(getColor(R.color.profile_edit_password_btn_border_active))
            binding.profileNicknameEt.isFocusableInTouchMode = true
            binding.profileNicknameEtLayout.error = null

            binding.profileDeleteUserTv.isClickable = false
        }
    }

    private fun convertDeleteUserMode() {
        if (viewModel.mFlagDeleteUser) {
            viewModel.mFlagDeleteUser = false
            binding.profileDeleteUserTv.text = getString(R.string.profile_delete_user)
            binding.profilePwEtLayout.visibility = View.GONE
            binding.profileDeleteUserCompleteBtn.visibility = View.GONE
            binding.profileDeleteUserCompleteBtn.isClickable = false
            viewModel.oldPassword.postValue("")
            binding.profilePwEt.clearFocus()
            binding.profilePwEtLayout.error = null

            binding.profileEditTv.isClickable = true
        } else {
            viewModel.mFlagDeleteUser = true
            binding.profileDeleteUserTv.text = getString(R.string.profile_delete_user_cancel)
            binding.profilePwEtLayout.visibility = View.VISIBLE
            binding.profileDeleteUserCompleteBtn.visibility = View.VISIBLE
            binding.profileDeleteUserCompleteBtn.isClickable = true

            binding.profileEditTv.isClickable = false
        }
    }

    private fun observeNickname() {
        viewModel.nickname.observe(this, Observer {
            if (it.isNotEmpty() && !Pattern.matches("^[a-zA-Z0-9가-힣]{1,10}\$", it)) {
                binding.profileNicknameEtLayout.error = getString(R.string.signup_nickname_validation)
            } else {
                binding.profileNicknameEtLayout.error = null
            }
        })
    }

    private fun observeProfileUrlFromFirebase() {
        viewModel.profileUrlFromFirebase.observe(this, Observer {
            if(it.isNotEmpty()) {
                patchProfile()
            }
        })
    }

    private fun pickImageFromGallery() {
        if (!viewModel.mFlagEdit) {
            return
        }

        binding.profileProfileImgIv.isClickable = false

        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, GlobalConstant.IMAGE_PICK_CODE)
    }

    private fun exifOrientationToDegree(exifOrientation: Int): Int {
        return when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotatePhoto(bitmap: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    //extract photo bitmap, date
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun extractPhoto(uri: Uri): Bitmap {
        lateinit var exif: ExifInterface
        lateinit var bitmap: Bitmap

        try {
            val inputStream = applicationContext.contentResolver.openInputStream(uri)
            val cursor = applicationContext.contentResolver.query(uri, null, null, null, null)

            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val dateTakenIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)

                if (c.moveToFirst()) {
                    val name = c.getString(nameIndex)

                    inputStream?.let { inputStream ->
                        val file = File(applicationContext.cacheDir, name)

                        val os = file.outputStream()

                        os.use {
                            inputStream.copyTo(it)
                        }

                        exif = ExifInterface(file.absolutePath)
                        val exifOrientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        val exifDegree = exifOrientationToDegree(exifOrientation)

                        bitmap =
                            rotatePhoto(BitmapFactory.decodeFile(file.absolutePath), exifDegree)
                    }
                }
            }
        } catch (e: Exception) {
            binding.profileRootLayout.snackbar("Error: " + e.message)
        }

        return bitmap
    }

    //2021-01-23T21:32:44.333
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == GlobalConstant.IMAGE_PICK_CODE) {
            if (data != null && data.data != null) {

                try {
                    val bitmap = extractPhoto(data.data!!)
                    Glide.with(this)
                        .load(bitmap)
                        .circleCrop()
                        .placeholder(R.drawable.character_big_basic)
                        .error(R.drawable.character_big_basic)
                        .into(binding.profileProfileImgIv)

                    viewModel.profileUrl.postValue(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startLoginActivity(){
        val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)
        finish()
    }

    private fun startCertifyEmailActivity() {
        binding.profileEditPasswordTv.isClickable = false
        val intent = Intent(this@ProfileActivity, CertifyEmailActivity::class.java)

        startActivity(intent)
    }

    override fun onPatchProfileStarted() {
        binding.profileLoading.show()
        binding.profileEditCompleteBtn.isClickable = false
    }

    override fun onPatchProfileSuccess() {
        binding.profileLoading.hide()
        binding.profileEditCompleteBtn.isClickable = true
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        convertEditMode()
    }

    override fun onPatchProfileFailure(code: Int, message: String) {
        binding.profileLoading.hide()

        when (code) {
            303 -> binding.profileRootLayout.snackbar(message)
            304 -> binding.profileNicknameEtLayout.error = message
            else -> binding.profileRootLayout.snackbar(getString(R.string.network_error))
        }

        binding.profileEditCompleteBtn.isClickable = true
    }

    override fun onUploadToFirebaseStarted() {
        binding.profileLoading.show()
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun onUploadToFirebaseSuccess() {
        binding.profileLoading.hide()
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun onUploadToFirebaseFailure() {
        binding.profileLoading.hide()

        binding.profileRootLayout.snackbar(getString(R.string.network_error))
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun onDeleteUserStarted() {
        binding.profileLoading.show()
        binding.profileDeleteUserCompleteBtn.isClickable = false
    }

    override fun onDeleteUserSuccess() {
        binding.profileLoading.hide()

        startLoginActivity()
    }

    override fun onDeleteUserFailure(code: Int, message: String) {
        binding.profileLoading.hide()

        when (code) {
            303, 304 -> binding.profilePwEtLayout.error = message
            else -> binding.profileRootLayout.snackbar(message)
        }

        binding.profileDeleteUserCompleteBtn.isClickable = true
    }
}