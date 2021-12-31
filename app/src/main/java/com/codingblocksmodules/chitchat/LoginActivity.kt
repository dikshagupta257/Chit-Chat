package com.codingblocksmodules.chitchat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.codingblocksmodules.chitchat.databinding.ActivityLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoginActivity : AppCompatActivity() {
    private lateinit var countryCode:String
    private lateinit var phoneNo:String
    private lateinit var binding:ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //enabling the next button only if length of user number is greater than or equal to 10
        binding.phoneNumberEt.addTextChangedListener {
            binding.nextBtn.isEnabled = !(it.isNullOrEmpty() || it.length<10)
        }

        //Hint Request - Assignment1
        binding.nextBtn.setOnClickListener{
            checkNumber()
        }
    }

    //validating the user number
    private fun checkNumber() {
        countryCode = binding.ccp.selectedCountryCodeWithPlus
        phoneNo = countryCode+ binding.phoneNumberEt.text.toString()

        //if number is validated then show the dialog box to notify the user
        if(validatePhoneNumber(binding.phoneNumberEt.text.toString())){
            notifyUser()
        }else{
            //else show the required error toast
            Toast.makeText(this,"Please enter a valid number to continue!",Toast.LENGTH_SHORT).show()
        }
    }

    //show the dialog box to notify the user
    private fun notifyUser() {
        MaterialAlertDialogBuilder(this).apply {
            setMessage("We will be verifying the phone number $phoneNo\n"+
                    "Is this OK, or would you like to edit the number?")
            setPositiveButton("Ok"){_,_ ->
                showOtpActivity()
            }
            setNegativeButton("edit"){dialog,_ ->
                dialog.dismiss()
            }
            setCancelable(false)
            create()
            show()
        }
    }

    //moving to next activity to verify number using otp
    private fun showOtpActivity() {
        startActivity(Intent(this, OtpActivity::class.java).putExtra(PHONE_NUMBER,phoneNo))
    }

    //helper function to check validity of user number
    private fun validatePhoneNumber(phone: String): Boolean {
        if(phone.isNotEmpty()){
            return true
        }
        return false
    }
}