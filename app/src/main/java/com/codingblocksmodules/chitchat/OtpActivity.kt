package com.codingblocksmodules.chitchat

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.codingblocksmodules.chitchat.databinding.ActivityOtpBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

const val PHONE_NUMBER = "phoneNumber"
class OtpActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityOtpBinding
    private lateinit var callbacks:PhoneAuthProvider.OnVerificationStateChangedCallbacks
    var mVerificationId:String? = null
    var mResendToken:PhoneAuthProvider.ForceResendingToken?=null
    private var phoneNumber:String?= null
    private lateinit var progressDialog: ProgressDialog
    private var mCounterDown:CountDownTimer?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        startVerify()

    }

    private fun startVerify() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber!!, // Phone number to verify
            60, // Timeout duration
            TimeUnit.SECONDS, // Unit of timeout
            this, // Activity (for callback binding)
            callbacks, // OnVerificationStateChangedCallbacks
        )
        showTimer(60000)
        progressDialog = createProgressDialog("Sending a verification code", false)
        progressDialog.show()
    }

    private fun showTimer(milliSecInFuture: Long) {
        binding.resendBtn.isEnabled = false
        mCounterDown = object : CountDownTimer(milliSecInFuture, 1000){
            override fun onTick(p0: Long) {
                //here you can have your logic to set text to edittext
                binding.counterTv.isVisible = true
                binding.counterTv.text = getString(R.string.seconds_remaining, p0/1000)
            }

            override fun onFinish() {
                binding.resendBtn.isEnabled = true
                binding.counterTv.isVisible = false
            }

        }.start()
    }

    private fun initViews() {
        phoneNumber = intent.getStringExtra(PHONE_NUMBER)
        binding.verifyTv.text = getString(R.string.verify_number, phoneNumber)
        setSpannableString()

        binding.verificationBtn.setOnClickListener(this)
        binding.resendBtn.setOnClickListener(this)

        // init firebase verify Phone number callback
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.

                if (::progressDialog.isInitialized) {
                    progressDialog.dismiss()
                }

                val smsCode = credential.smsCode
                if(!(smsCode.isNullOrBlank())){
                    binding.sentCodeEt.setText(smsCode)
                }
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                if (::progressDialog.isInitialized) {
                    progressDialog.dismiss()
                }

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }

                // Show a message and update the UI
                notifyUserAndRetry("Your Phone Number might be wrong or connection error.Retry again!")

            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                if (::progressDialog.isInitialized) {
                    progressDialog.dismiss()
                }
                binding.counterTv.isVisible = false
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId
                mResendToken = token
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        val mAuth = FirebaseAuth.getInstance()
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this){
                if(it.isSuccessful){
                    if (::progressDialog.isInitialized) {
                        progressDialog.dismiss()
                    }
                    showSignUpActivity()
                }else{
                    if (::progressDialog.isInitialized) {
                        progressDialog.dismiss()
                    }
                    notifyUserAndRetry("Your Phone Number Verification is failed.Retry again!")
                }
            }
    }

    private fun notifyUserAndRetry(message: String) {
        MaterialAlertDialogBuilder(this).apply {
            setMessage(message)
            setPositiveButton("Ok"){_,_->
                showLoginActivity()
            }
            setNegativeButton("Cancel"){dialog, _ ->
                dialog.dismiss()
            }
            setCancelable(false)
            create()
            show()
        }
    }

    private fun setSpannableString() {
        val span = SpannableString(getString(R.string.waiting_text, phoneNumber))
        val clickableSpan = object : ClickableSpan(){

            // handle click event
            override fun onClick(p0: View) {
                //send user back to Login Activity
                showLoginActivity()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false // you can use custom color
                ds.color = ds.linkColor // this remove the underline
            }
        }

        span.setSpan(clickableSpan,span.length-13,span.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.waitingTv.movementMethod = LinkMovementMethod.getInstance()
        binding.waitingTv.text = span
    }

    private fun showLoginActivity() {
        startActivity(Intent(this,LoginActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    private fun showSignUpActivity() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(mCounterDown!=null){
            mCounterDown!!.cancel()
        }
    }

    override fun onBackPressed() {
        //removed super call
    }

    override fun onClick(p0: View?) {
        when(p0){
            binding.verificationBtn ->{
                var code = binding.sentCodeEt.text.toString()
                if(code.isNotEmpty()&&!mVerificationId.isNullOrEmpty()){

                    progressDialog = createProgressDialog("Please wait.", false)
                    progressDialog.show()
                    val credential = PhoneAuthProvider.getCredential(mVerificationId!!, code)
                    signInWithPhoneAuthCredential(credential)

                }
            }
            binding.resendBtn->{
                if(mResendToken!=null){
                    showTimer(60000)
                    progressDialog = createProgressDialog("Sending a verification code", false)
                    progressDialog.show()

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        phoneNumber!!, // Phone number to verify
                        60, // Timeout duration
                        TimeUnit.SECONDS, // Unit of timeout
                        this, // Activity (for callback binding)
                        callbacks, // OnVerificationStateChangedCallbacks
                    )
                }else {
                    Toast.makeText(this,"Sorry, You Can't request new code now, Please wait ...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

fun Context.createProgressDialog(message:String, isCancelable:Boolean):ProgressDialog {
    return ProgressDialog(this).apply {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setMessage(message)
    }
}