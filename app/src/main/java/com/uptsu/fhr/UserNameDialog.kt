package com.uptsu.fhr

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*

class UserNameDialog(
    private val context: Context,
    private val onNameEntered: (String, Int, String, String?, String?, String, Map<String, Any>) -> Unit,
    private val saveCallback: () -> Unit
) {

    fun show(triggeringButton: String, existingName: String? = null, existingDilation: Int? = null) {
        val inflater = LayoutInflater.from(context)
        val dialogView: View = inflater.inflate(R.layout.dialog_user_details, null)

        // Find views
        val spinnerIdType: Spinner = dialogView.findViewById(R.id.spinnerIdType)
        val input: EditText = dialogView.findViewById(R.id.editTextUserName)
        val minusButton: Button = dialogView.findViewById(R.id.buttonMinus)
        val plusButton: Button = dialogView.findViewById(R.id.buttonPlus)
        val valueTextView: TextView = dialogView.findViewById(R.id.textViewDilationValue)
        val patientNameInput: EditText = dialogView.findViewById(R.id.editTextPatientName)
        val mobileNoInput: EditText = dialogView.findViewById(R.id.editTextMobileNo)
        val spinnerFacility: Spinner = dialogView.findViewById(R.id.spinnerFacility)

        // Setup Spinner dropdown options
        val idOptions = arrayOf(
            "Select the ID type",
            "ABHA ID",
            "RCH ID",
            "E-kavach ID",
            "Hospital ID"
        )
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, idOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerIdType.adapter = adapter

        // Hospital names for facility spinner
        val hospitalOptions = arrayOf(
            "Select the facility",
            "FARRUKHBAD-CHC-KAIMGANJ",
            "FARRUKHBAD-CHC-KAMALGANJ",
            "LUCKNOW-CHC-MALL",
            "LUCKNOW-CHC-ITAUNJA",
            "MIRZAPUR-CHC-MARIHAN",
            "MIRZAPUR-CHC-PAHARI",
            "MIRZAPUR-CHC-LALGANJ",
            "RAEBARELI-CHC-BACHHRAWAN",
            "RAEBARELI-CHC-MAHARAJGANJ",
            "RAEBARELI-CHC-DALMAU",
            "VARANASI-CHC-ARAJILINE",
            "VARANASI-CHC-CHOLAPUR"
        )
        val facilityAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, hospitalOptions)
        facilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFacility.adapter = facilityAdapter

        // Set existing values
        valueTextView.text = (existingDilation ?: 4).toString()

        // Hide additional fields initially
        patientNameInput.visibility = View.GONE
        mobileNoInput.visibility = View.GONE

        lateinit var positiveButton: Button  // Store reference to the OK button

        // Spinner Selection Change
        spinnerIdType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                input.hint= idOptions[position]
                if (idOptions[position] == "Hospital ID" || idOptions[position] == "RCH ID") {
                    patientNameInput.visibility = View.VISIBLE
                    mobileNoInput.visibility = View.VISIBLE
                } else {
                    patientNameInput.visibility = View.GONE
                    mobileNoInput.visibility = View.GONE
                }
                validateFields(positiveButton, input, patientNameInput, mobileNoInput, idOptions[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Handle - Button click
        minusButton.setOnClickListener {
            val currentValue = valueTextView.text.toString().toInt()
            if (currentValue > 4) {
                valueTextView.text = (currentValue - 1).toString()
            }
        }

        // Handle + Button click
        plusButton.setOnClickListener {
            val currentValue = valueTextView.text.toString().toInt()
            if (currentValue < 10) {
                valueTextView.text = (currentValue + 1).toString()
            }
        }

        // Create AlertDialog
        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)

        // Handle OK button
        builder.setPositiveButton("OK", null)

        // Create and show dialog
        val dialog = builder.create()
        dialog.setOnShowListener {
            positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled = false  // Initially disabled

            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    validateFields(positiveButton, input, patientNameInput, mobileNoInput, spinnerIdType.selectedItem.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            }

            input.addTextChangedListener(textWatcher)
            patientNameInput.addTextChangedListener(textWatcher)
            mobileNoInput.addTextChangedListener(textWatcher)

            // Validate ID and facility selection
            positiveButton.setOnClickListener {
                val selectedIdType = spinnerIdType.selectedItem.toString()
                val selectedFacility = spinnerFacility.selectedItem.toString()

                val facilityError: TextView = dialogView.findViewById(R.id.facilityError)
                val idTypeError: TextView = dialogView.findViewById(R.id.idTypeError)

                var isValid = true

                if (selectedFacility == "Select the facility") {
                    facilityError.visibility = View.VISIBLE
                    isValid = false
                } else {
                    facilityError.visibility = View.GONE
                }

                if (selectedIdType == "Select the ID type") {
                    idTypeError.visibility = View.VISIBLE
                    isValid = false
                } else {
                    idTypeError.visibility = View.GONE
                }

                if (!isValid) return@setOnClickListener

                // Proceed with the rest of the logic
                val userId = input.text.toString()
                val dilationValue = valueTextView.text.toString().toInt()
                val patientName = patientNameInput.text.toString().takeIf { it.isNotEmpty() } ?: ""
                val mobileNo = mobileNoInput.text.toString().takeIf { it.isNotEmpty() } ?: ""

                // ✅ Updated Map with New Structure
                val userData = mutableMapOf<String, Any>(
                    "id type" to selectedIdType,  // Renamed from "idType"
                    "id" to userId,  // Taken from input field
                    "pname" to patientName,  // Taken from patientNameInput
                    "mobile" to mobileNo,  // Taken from mobileNoInput
                    "dilation" to dilationValue,  // Taken from valueTextView
                    "facility" to selectedFacility, // Added facility
                    "total_time" to "00:00:00"  // Default total time (modify if needed)
                )

                onNameEntered(
                    userId,
                    dilationValue,
                    selectedIdType,
                    patientName,
                    mobileNo,
                    triggeringButton,
                    userData // <-- pass the map
                )

                saveCallback()
                dialog.dismiss()

                // ✅ Start Listening Automatically
                if (triggeringButton == "yourButton") {
                    val mainActivity = context as? MainActivity
                    mainActivity?.startButton?.performClick()
                }
            }

        }

        builder.setCancelable(false)
        dialog.show()
    }

    // ✅ **Validation function to enable/disable OK button**
    private fun validateFields(
        positiveButton: Button,
        userNameInput: EditText,
        patientNameInput: EditText,
        mobileNoInput: EditText,
        selectedIdType: String
    ) {
        val userNameValid = userNameInput.text.toString().length >= 1

        val patientNameValid =
            if (selectedIdType == "Hospital ID" || selectedIdType == "RCH ID") patientNameInput.text.toString().matches(Regex("^[a-zA-Z ]{1,}$"))
            else true

        val mobileNoValid =
            if (selectedIdType == "Hospital ID"  || selectedIdType == "RCH ID") mobileNoInput.text.toString().matches(Regex("^[0-9]{10}$"))
            else true

        positiveButton.isEnabled = userNameValid && patientNameValid && mobileNoValid
    }

}
