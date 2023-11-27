package com.miniproject.uber

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.miniproject.uber.Model.DriverInfoModel
import com.miniproject.uber.databinding.ActivityVerifyPhoneBinding

class VerifyPhoneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyPhoneBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var phoneNumber: String
    private lateinit var firstName: String
    private lateinit var lastName: String
    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference
    private val TAG = "VerifyPhoneActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyPhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE)
        firebaseAuth = FirebaseAuth.getInstance()

        // Retrieve the phone number from the intent
        phoneNumber = intent.getStringExtra("number")!!
        firstName = intent.getStringExtra("firstName")!!
        lastName = intent.getStringExtra("lastName")!!

        // Call the function to start phone number verification
        startPhoneNumberVerification(phoneNumber)

        binding.btnVerifyCode.setOnClickListener {
            val code = binding.etVerificationCode.text.toString()
            if (code.isNotEmpty()) {
                // Use the verification ID and code to create PhoneAuthCredential
                val credential = PhoneAuthProvider.getCredential(storedVerificationId, code)
                signInWithPhoneAuthCredential(credential)
            } else {
                Toast.makeText(this, "Enter the verification code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "onVerificationCompleted:$credential")
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.w(TAG, "onVerificationFailed", e)

//                    if (e is FirebaseAuthInvalidCredentialsException) {
//                        // Invalid request
//                    } else if (e is FirebaseTooManyRequestsException) {
//                        // The SMS quota for the project has been exceeded
//                    } else if (e is FirebaseAuthMissingActivityForRecaptchaException) {
//                        // reCAPTCHA verification attempted with null Activity
//                    }

                    // Show a message and update the UI
                    Toast.makeText(this@VerifyPhoneActivity, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken,
                ) {
                    Log.d(TAG, "onCodeSent:$verificationId")

                    // Save verification ID and resending token so we can use them later
                    storedVerificationId = verificationId
                    resendToken = token
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        val model = DriverInfoModel()
                        model.firstName = firstName
                        model.lastName = lastName
                        model.phoneNumber = phoneNumber
                        model.rating = 0.0

                        driverInfoRef.child(user.uid)
                            .setValue(model)
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "" + e.message, Toast.LENGTH_SHORT).show()
                            }
                            .addOnSuccessListener {
                                Toast.makeText(this, "Register Successfully", Toast.LENGTH_SHORT).show()
                                goToHomeActivity(model)
                            }
                    } else {
                        // Handle the case where the user is null
                        Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    val exception = task.exception
                    Toast.makeText(this, "Phone sign-in failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
                    exception?.printStackTrace()
                }
            }
    }

    private fun goToHomeActivity(model: DriverInfoModel) {
        Common.currentUser = model
        startActivity(Intent(this, DriverHomeActivity::class.java))
        finish()
    }
}
