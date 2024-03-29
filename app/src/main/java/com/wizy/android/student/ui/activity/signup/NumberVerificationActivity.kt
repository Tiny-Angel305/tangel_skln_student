package com.wizy.android.student.ui.activity.signup

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.wizy.android.student.R
import com.wizy.android.student.base.BaseToolbarActivity
import com.wizy.android.student.helper.AppConstants
import com.wizy.android.student.helper.CommonUtils
import com.wizy.android.student.model.Student
import kotlinx.android.synthetic.main.activity_number_verification.*
import java.util.concurrent.TimeUnit

class NumberVerificationActivity : BaseToolbarActivity(), View.OnClickListener {
    private var user: Student? = null
    private var mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private val callback: PhoneAuthProvider.OnVerificationStateChangedCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                /*  if (credential.smsCode != null) {
                      etCode.setText(credential.smsCode)
                      signInWithPhoneAuthCredential(credential)
                  }*/
            }

            override fun onVerificationFailed(e: FirebaseException) {
                hideProgress()
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> Toast.makeText(
                        this@NumberVerificationActivity,
                        e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    is FirebaseTooManyRequestsException -> Toast.makeText(
                        this@NumberVerificationActivity,
                        e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    else -> Toast.makeText(this@NumberVerificationActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCodeSent(verificationId: String?, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendToken = token
                onVerificationCodeSent()
            }
        }

    private fun sendVerificationCode() {
        showProgress()
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            user?.number!!, 60, TimeUnit.SECONDS, this, callback
        )
    }

    private fun onVerificationCodeSent() {
        hideProgress()
        Snackbar.make(tvNumber, getString(R.string.code_sent), Snackbar.LENGTH_SHORT).show()
        Handler().postDelayed({
            tvNumber.visibility = View.GONE
            underline.visibility = View.GONE
            tvVerifyHint.visibility = View.GONE
            btnSendCode.visibility = View.GONE
            hiddenLayout.visibility = View.VISIBLE
            startTimer()
        }, 800)

    }

    private fun startTimer() {
        btnResend.isEnabled = false
        object : CountDownTimer(TimeUnit.MINUTES.toMillis(1), 1000) {
            override fun onFinish() {
                btnResend.isEnabled = true
                btnResend.text = getString(R.string.resend)
            }

            override fun onTick(millisUntilFinished: Long) {
                btnResend.text = CommonUtils.getTimerText(millisUntilFinished)
            }
        }.start()
    }

    private fun verifyPhoneNumber() {
        when {
            pinView.text.isNullOrEmpty() -> {
                Snackbar.make(btnVerify, getString(R.string.enter_valid_code), Snackbar.LENGTH_SHORT).show()
            }
            else -> {
                showProgress()
                val otp: String = pinView.text.toString()
                val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId!!, otp)
                signInWithPhoneAuthCredential(credential)
            }
        }

    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    hideProgress()
                    onNumberVerification()
                } else {
                    hideProgress()
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Snackbar.make(tvNumber, getString(R.string.invalid_code), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun onNumberVerification() {
        Snackbar.make(tvNumber, getString(R.string.number_verification_successful), Snackbar.LENGTH_SHORT).show()
        Handler().postDelayed({
            startActivity(
                Intent(this, GenderSelectionActivity::class.java)
                    .putExtra(AppConstants.INTENT_USER, user)
            )
        }, 600)
    }

    private fun resendVerificationCode() {
        showProgress()
        resendToken?.let {
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                user?.number!!,
                60,
                TimeUnit.SECONDS,
                this,
                callback,
                it
            )
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_verification)
        getIntentData()

    }

    private fun setUpView() {
        tvNumber.text = user?.number
        tvNumber_1.text = user?.number
        btnSendCode.setOnClickListener(this)
        btnVerify.setOnClickListener(this)
        btnResend.setOnClickListener(this)

//        btnSendCode.callOnClick();
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            user?.number!!, 60, TimeUnit.SECONDS, this, callback
        )
    }

    private fun getIntentData() {
        if (intent.hasExtra(AppConstants.INTENT_USER)) {
            user = intent.getSerializableExtra(AppConstants.INTENT_USER) as Student
            setUpView()
        } else {
            showIntentIsNull()
        }

    }

    private fun showIntentIsNull() {
        Toast.makeText(this, getString(R.string.intent_is_null), Toast.LENGTH_SHORT).show()
        this.finish()
    }

    override fun onClick(v: View?) {
        when (v) {
            btnSendCode -> sendVerificationCode()
            btnVerify -> verifyPhoneNumber()
            btnResend -> {
                resendVerificationCode()
            }
        }
    }
}
