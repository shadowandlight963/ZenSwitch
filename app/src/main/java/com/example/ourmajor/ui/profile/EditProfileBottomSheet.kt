package com.example.ourmajor.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.hbb20.CountryCodePicker
import com.example.ourmajor.R
import com.example.ourmajor.data.profile.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditProfileBottomSheet : BottomSheetDialogFragment() {

    private lateinit var vm: ProfileViewModel2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        _savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, _savedInstanceState: Bundle?) {
        super.onViewCreated(view, _savedInstanceState)

        vm = ViewModelProvider(
            requireActivity(),
            ProfileViewModelFactory(requireContext())
        )[ProfileViewModel2::class.java]

        val nameEdit = view.findViewById<TextInputEditText>(R.id.nameEdit)
        val bioEdit = view.findViewById<TextInputEditText>(R.id.bioEdit)
        val ageEdit = view.findViewById<TextInputEditText>(R.id.ageEdit)
        val mobileEdit = view.findViewById<TextInputEditText>(R.id.mobileEdit)
        val levelEdit = view.findViewById<TextInputEditText>(R.id.levelEdit)
        val goalEdit = view.findViewById<TextInputEditText>(R.id.goalEdit)

        val bioLayout = view.findViewById<TextInputLayout>(R.id.bioLayout)
        val levelLayout = view.findViewById<TextInputLayout>(R.id.levelLayout)
        val goalLayout = view.findViewById<TextInputLayout>(R.id.goalLayout)
        val ccp = view.findViewById<CountryCodePicker>(R.id.ccp)

        val genderDropdown = view.findViewById<android.widget.AutoCompleteTextView>(R.id.genderDropdown)

        val genderItems = listOf("Male", "Female", "Non-Binary", "Prefer not to say")
        genderDropdown?.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, genderItems)
        )

        ccp.registerCarrierNumberEditText(mobileEdit)

        data class PhoneParts(
            val countryCodeWithPlus: String,
            val localDigits: String,
            val e164: String
        )

        fun buildPhoneParts(): PhoneParts {
            val countryCode = runCatching { ccp.selectedCountryCodeWithPlus }
                .getOrElse { "+${ccp.selectedCountryCode}" }

            val rawLocal = mobileEdit.text?.toString().orEmpty()
            val localDigits = rawLocal.replace(Regex("[^0-9]"), "")

            val ccDigits = countryCode.replace("+", "")
            val normalizedLocal = if (localDigits.startsWith(ccDigits) && localDigits.length > ccDigits.length + 3) {
                localDigits.drop(ccDigits.length)
            } else {
                localDigits
            }

            val e164 = if (normalizedLocal.isBlank()) "" else (countryCode + normalizedLocal)
            return PhoneParts(countryCode, normalizedLocal, e164)
        }

        fun getEnteredPhoneE164(): String {
            val parts = buildPhoneParts()
            return parts.e164
        }

        val current = vm.uiState.value?.profile
        if (current != null) {
            nameEdit.setText(current.displayName)
            bioEdit.setText(current.bio)
            ageEdit.setText(current.age?.toString().orEmpty())
            if (current.mobileNumber.trim().startsWith("+")) {
                runCatching {
                    ccp.setFullNumber(current.mobileNumber.trim().drop(1))
                }.onFailure {
                    mobileEdit.setText(current.mobileNumber)
                }
            } else {
                mobileEdit.setText(current.mobileNumber)
            }
            levelEdit.setText(current.mindfulnessLevel)
            goalEdit.setText(current.primaryGoal)

            val g = current.gender.trim()
            if (g.isNotBlank()) {
                genderDropdown?.setText(g, false)
            }
        }

        val earthyAccent = ContextCompat.getColor(requireContext(), R.color.sage_primary)
        levelLayout?.setEndIconTintList(android.content.res.ColorStateList.valueOf(earthyAccent))
        goalLayout?.setEndIconTintList(android.content.res.ColorStateList.valueOf(earthyAccent))

        val archetypeOptions = arrayOf(
            "The Drifter (Beginner)",
            "The Sprout (Starting)",
            "The Observer (Aware)",
            "The Anchor (Grounded)",
            "The Sage (Master)"
        )
        val northStarOptions = arrayOf(
            "Inner Calm",
            "Deep Focus",
            "Reclaiming Time",
            "Digital Detox",
            "Present Moment"
        )

        levelLayout?.setEndIconOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Choose your Zen Archetype")
                .setItems(archetypeOptions) { _, which ->
                    levelEdit.setText(archetypeOptions[which])
                    levelEdit.setSelection(levelEdit.text?.length ?: 0)
                }
                .show()
        }

        goalLayout?.setEndIconOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Choose your North Star")
                .setItems(northStarOptions) { _, which ->
                    goalEdit.setText(northStarOptions[which])
                    goalEdit.setSelection(goalEdit.text?.length ?: 0)
                }
                .show()
        }

        bioLayout?.setEndIconOnClickListener {
            val quotes = listOf(
                "Inhale the future, exhale the past.",
                "Soft mind, strong heart.",
                "Slow down. You are here.",
                "Let go, and let peace in.",
                "One breath. One moment.",
                "Be where your feet are.",
                "Peace begins with a breath.",
                "This moment is enough.",
                "Return to the present.",
                "Let your mind settle like still water.",
                "Choose calm over chaos.",
                "Gentle is powerful.",
                "Notice. Breathe. Release.",
                "A quiet mind is a clear mind.",
                "What you seek is within.",
                "Move slowly, live deeply.",
                "Breathe in peace. Breathe out worry.",
                "Let go of what you can't control.",
                "Begin again—gently.",
                "Small steps, every day.",
                "Progress over perfection.",
                "Let your breath be your anchor.",
                "You are safe in this moment.",
                "Calm is a superpower.",
                "Let the day be light."
            )
            bioEdit.setText(quotes.random())
            bioEdit.setSelection(bioEdit.text?.length ?: 0)
        }

        view.findViewById<View>(R.id.saveButton).setOnClickListener {
            val displayName = nameEdit.text?.toString().orEmpty().trim()
            if (displayName.isBlank()) {
                Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageEdit.text?.toString()?.trim().orEmpty().toIntOrNull()

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val latest = vm.uiState.value?.profile
            val safeBase = (latest ?: current ?: UserProfile()).copy(
                uid = (latest?.uid?.takeIf { it.isNotBlank() } ?: current?.uid?.takeIf { it.isNotBlank() } ?: user.uid),
                email = (latest?.email?.takeIf { it.isNotBlank() } ?: current?.email?.takeIf { it.isNotBlank() } ?: user.email.orEmpty()),
                photoUrl = (latest?.photoUrl ?: current?.photoUrl ?: user.photoUrl?.toString())
            )

            val enteredNumber = getEnteredPhoneE164()

            val gender = genderDropdown?.text?.toString().orEmpty().trim()

            val updated = safeBase.copy(
                displayName = displayName,
                bio = bioEdit.text?.toString().orEmpty().trim(),
                age = age,
                gender = gender,
                mobileNumber = enteredNumber,
                mindfulnessLevel = levelEdit.text?.toString().orEmpty().trim(),
                primaryGoal = goalEdit.text?.toString().orEmpty().trim()
            )

            vm.saveProfile(updated)
            dismiss()
        }
    }
}
