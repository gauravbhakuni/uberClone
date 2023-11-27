package com.miniproject.uber

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.miniproject.uber.Model.DriverInfoModel
import com.miniproject.uber.databinding.ActivityNumberBinding

class NumberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNumberBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var phoneNumberUtil: PhoneNumberUtil
    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        phoneNumberUtil = PhoneNumberUtil.getInstance()
        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE)

        if (auth.currentUser != null) {
            startActivity(Intent(this, DriverHomeActivity::class.java))
            finish()
        }

        // Populate the country codes for the AutoCompleteTextView
        val countryCodes = phoneNumberUtil.supportedRegions.map { region ->
            "+" + phoneNumberUtil.getCountryCodeForRegion(region)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countryCodes)
        binding.autoCompleteCountryCode.setAdapter(adapter)

        binding.btnVerifyNumber.setOnClickListener {
            val phoneNumber = binding.phoneNumber.text.toString().trim()
            val firstName = binding.firstName.text.toString()
            val lastName = binding.lastName.text.toString()
            val selectedCountryCode = binding.autoCompleteCountryCode.text.toString()

            if (firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty() || selectedCountryCode.isEmpty()) {
                Toast.makeText(this, "The fields can't be empty!!", Toast.LENGTH_SHORT).show()
            } else {
                val formattedPhoneNumber = formatPhoneNumber(selectedCountryCode, phoneNumber)

                // Check if the driver is already registered
                checkIfDriverExists(formattedPhoneNumber, firstName, lastName)
            }
        }
    }

    private fun checkIfDriverExists(phoneNumber: String, firstName: String, lastName: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            driverInfoRef
                .child(currentUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@NumberActivity, error.message, Toast.LENGTH_SHORT).show()
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val model = snapshot.getValue(DriverInfoModel::class.java)
                            goToHomeActivity(model)
                        } else {
                            val intent = Intent(this@NumberActivity, VerifyPhoneActivity::class.java)
                            intent.putExtra("number", phoneNumber)
                            intent.putExtra("firstName", firstName)
                            intent.putExtra("lastName", lastName)
                            startActivity(intent)
                        }
                    }
                })
        } else {
            val intent = Intent(this@NumberActivity, VerifyPhoneActivity::class.java)
            intent.putExtra("number", phoneNumber)
            intent.putExtra("firstName", firstName)
            intent.putExtra("lastName", lastName)
            startActivity(intent)
        }
    }


    private fun goToHomeActivity(model: DriverInfoModel?) {
        Common.currentUser = model
        startActivity(Intent(this, DriverHomeActivity::class.java))
        finish()
    }

    private fun formatPhoneNumber(countryCode: String, phoneNumber: String): String {
        val numberProto = Phonenumber.PhoneNumber()
        numberProto.countryCode = phoneNumberUtil.getCountryCodeForRegion(
            phoneNumberUtil.getRegionCodeForCountryCode(countryCode.substring(1).toInt())
        )
        numberProto.nationalNumber = phoneNumber.toLong()
        return phoneNumberUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164)
    }
}
