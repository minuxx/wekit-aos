package com.coconutplace.wekit.ui.signup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent.*
import android.view.View
import android.widget.CheckBox
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.coconutplace.wekit.R
import com.coconutplace.wekit.data.remote.auth.listeners.SignUpListener
import com.coconutplace.wekit.databinding.ActivitySignupBinding
import com.coconutplace.wekit.ui.BaseActivity
import com.coconutplace.wekit.ui.WekitDialog
import com.coconutplace.wekit.ui.poll.PollActivity
import com.coconutplace.wekit.utils.hide
import com.coconutplace.wekit.utils.hideKeyboard
import com.coconutplace.wekit.utils.show
import com.coconutplace.wekit.utils.snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.regex.Pattern


class SignUpActivity : BaseActivity(), SignUpListener, WekitDialog.WekitDialogClickListener {
    private lateinit var binding: ActivitySignupBinding
    private val viewModel: SignUpViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_signup)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        viewModel.signUpListener = this

        observeEmail()
        observeNickname()
        observeBirthday()
        observeId()
        observePw()
        observePwCheck()
        observeTncAgree()

        binding.signupRootLayout.setOnClickListener(this)
        binding.signupBackBtn.setOnClickListener(this)


//        binding.signupBirthdayEt.setOnKeyListener(View.OnKeyListener{ v, keyCode, event ->
//            if(keyCode == KeyEvent.KEYCODE_DEL){
//
//            } else if(keyCode >= KeyEvent.KEYCODE_0 )
//
//            false
//        })

        binding.signupBirthdayEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                binding.signupBirthdayEt.setSelection(s!!.length)
            }
        })

        binding.signupBirthdayEt.setOnKeyListener{ v, keycode, event ->
            if(event.action == ACTION_DOWN && keycode == KEYCODE_BACK){

            } else if(event.action == ACTION_DOWN && (keycode in KEYCODE_0..KEYCODE_9)){
                val bd = viewModel.birthday.value!!.toString()

                if(bd.length == 3 || bd.length == 6){
                    val ch = event.unicodeChar.toChar()
                    viewModel.birthday.postValue("$bd$ch-")
                }
            }

            false
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        when(v){
            binding.signupRootLayout -> binding.signupRootLayout.hideKeyboard()
            binding.signupBackBtn -> finish()
        }
    }

    private fun observeEmail() {
        viewModel.email.observe(this, Observer {
            if (it.isNotEmpty() && !Pattern.matches(
                    "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.0-]+\\.[a-zA-Z]{2,6}$",
                    it
                )
            ) {
                binding.signupEmailEtLayout.error = getString(R.string.signup_email_validation)
            } else {
                binding.signupEmailEtLayout.error = null
            }
        })
    }

    private fun observeId() {
        viewModel.id.observe(this, Observer {
            if (it.isNotEmpty() && !Pattern.matches("^[a-zA-Z]+[a-zA-Z0-9]{5,15}\$", it)) {
                binding.signupIdEtLayout.error = getString(R.string.signup_id_validation)
            } else {
                binding.signupIdEtLayout.error = null
            }
        })
    }

    private fun observePw() {
        viewModel.pw.observe(this, Observer {
            if (it.isNotEmpty() && !Pattern.matches(
                    "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[\$@\$!%*#?&^])[A-Za-z[0-9]\$@\$!%*#?&]{10,16}\$",
                    it
                )
            ) {
                binding.signupPwEtLayout.error = getString(R.string.password_validation)
            } else {
                binding.signupPwEtLayout.error = null
            }
        })
    }

    private fun observePwCheck() {
        viewModel.pwCheck.observe(this, Observer {
            if (it.isNotEmpty() && it != viewModel.pw.value.toString()) {
                binding.signupPwCheckEtLayout.error = getString(R.string.password_check)
            } else {
                binding.signupPwCheckEtLayout.error = null
            }
        })
    }

    private fun observeNickname() {
        viewModel.nickname.observe(this, Observer {
            if (it.isNotEmpty() && !Pattern.matches("^[a-zA-Z0-9가-힣]{1,10}\$", it)) {
                binding.signupNicknameEtLayout.error =
                    getString(R.string.signup_nickname_validation)
            } else {
                binding.signupNicknameEtLayout.error = null
            }
        })
    }

    fun onGenderCheckboxClicked(view: View) {
        when(view as CheckBox){
            binding.signupManCb -> {
                binding.signupManCb.setBackgroundResource(R.drawable.checkbox_checked)
                binding.signupWomanCb.setBackgroundResource(R.drawable.checkbox_unchecked)
                binding.signupWomanCb.isChecked = false
            }
            binding.signupWomanCb -> {
                binding.signupWomanCb.setBackgroundResource(R.drawable.checkbox_checked)
                binding.signupManCb.setBackgroundResource(R.drawable.checkbox_unchecked)
                binding.signupManCb.isChecked = false
            }
        }
    }

    private fun observeBirthday() {
        viewModel.birthday.observe(this, Observer {
            if (it.isNotEmpty() && !Pattern.matches(
                    "^(19[0-9][0-9]|20\\d{2})-(0[0-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])\$",
                    it
                )
            ) {
                binding.signupBirthdayEtLayout.error =
                    getString(R.string.signup_birthday_validation)
            } else {
                binding.signupBirthdayEtLayout.error = null
            }
        })
    }

    private fun observeTncAgree(){
        viewModel.tncAgree.observe(this, Observer {
            if (it) {
                binding.signupTncCb.setBackgroundResource(R.drawable.checkbox_checked)
            } else {
                binding.signupTncCb.setBackgroundResource(R.drawable.checkbox_unchecked)
            }
        })
    }

    override fun onOKClicked() {

    }

    override fun onSignUpStarted() {
        binding.signupLoading.show()
        binding.signupCompleteBtn.isClickable = false
    }

    override fun onSignUpSuccess(message: String) {
        binding.signupLoading.hide()

        val signUpIntent = Intent(this, PollActivity::class.java)
        startActivity(signUpIntent)
        finish()
    }

    override fun onSignUpFailure(code: Int, message: String) {
        binding.signupLoading.hide()

        when(code){
            301, 302, 303 -> binding.signupIdEtLayout.error = message
            304, 305, 306 -> binding.signupPwEtLayout.error = message
            307, 308 -> binding.signupEmailEtLayout.error = message
            309, 310, 311, 317 -> binding.signupNicknameEtLayout.error = message
            315 -> binding.signupBirthdayEtLayout.error = message
            319 -> showDialog(message)
            404 -> showDialog(getString(R.string.network_error))
            else -> binding.signupRootLayout.snackbar(message)
        }

        binding.signupCompleteBtn.isClickable = true
    }
}