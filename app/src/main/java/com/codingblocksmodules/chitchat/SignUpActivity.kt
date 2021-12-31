package com.codingblocksmodules.chitchat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.codingblocksmodules.chitchat.databinding.ActivitySignUpBinding
import com.codingblocksmodules.chitchat.models.User
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private val storage by lazy {
        FirebaseStorage.getInstance()
    }
    private val auth  by lazy{
        FirebaseAuth.getInstance()
    }
    private val database by lazy{
        FirebaseFirestore.getInstance()
    }
    private lateinit var downloadUrl:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Assignment 2 - Add Crop Image option along with a thumbnail using firebase extension
        binding.userImgView.setOnClickListener {
            checkPermissionForImage()
        }

        binding.nextBtn.setOnClickListener{
            binding.nextBtn.isEnabled = false
            val name = binding.nameEt.text.toString()
            if(name.isEmpty()){
                Toast.makeText(this,"Name cannot be empty",Toast.LENGTH_SHORT).show()
            }else if(!::downloadUrl.isInitialized){
             Toast.makeText(this,"Image cannot be empty",Toast.LENGTH_SHORT).show()
            }else{
                val user = User(name, downloadUrl, downloadUrl, auth.uid!!)
                database.collection("users").document(auth.uid!!).set(user)
                    .addOnSuccessListener {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }.addOnFailureListener {
                        binding.nextBtn.isEnabled = true
                    }
            }

        }
    }

    private fun checkPermissionForImage() {
        if((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED)&&
            (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED)
        ){
            val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            val permissionWrite = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

            requestPermissions(permission,
            1001)

            requestPermissions(permissionWrite,
            1002)
        }else{
            pickImageFromGallery()
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"

        startActivityForResult(
            intent,
            1000
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == 1000){
            data?.data?.let{
                binding.userImgView.setImageURI(it)
                uploadImage(it)
            }
        }
    }

    private fun uploadImage(it: Uri) {
        binding.nextBtn.isEnabled = false

        val ref = storage.reference.child("uploads/"+auth.uid.toString())
        val uploadTask = ref.putFile(it)

        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task->
            if(!task.isSuccessful){
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        }).addOnCompleteListener { task ->
            binding.nextBtn.isEnabled = true
            if(task.isSuccessful){
                downloadUrl = task.result.toString()
                Log.i("URL","downloadUrl: $downloadUrl")
            }else{

            }
        }.addOnFailureListener{
            Toast.makeText(this, "Uploading image failed", Toast.LENGTH_SHORT).show()
        }
    }
}