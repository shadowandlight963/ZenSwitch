package com.example.ourmajor.auth

import android.content.Intent
import android.app.ActivityOptions
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import com.example.ourmajor.MainActivity
import com.example.ourmajor.R
import com.example.ourmajor.databinding.ActivityAuthBinding
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var authViewModel: AuthViewModel

    private var mode: Mode = Mode.LOGIN
    private var hasNavigated: Boolean = false
    private var lastErrorMessage: String? = null
    private var validationWired: Boolean = false
    private var lastVerificationSentTo: String? = null
    private var showingNeedsVerificationDialog: Boolean = false

    enum class Mode { LOGIN, SIGN_UP }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        render()
        wireClicks()
        wireValidation()
        observe()

        animateEntrance()
    }

    private fun animateEntrance() {
        val dy = (resources.displayMetrics.density * 18f)
        binding.content.alpha = 0f
        binding.content.translationY = dy
        binding.content.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(420)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun wireClicks() {
        binding.primaryButton.setOnClickListener {
            val email = binding.emailEdit.text?.toString().orEmpty()
            val password = binding.passwordEdit.text?.toString().orEmpty()

            if (mode == Mode.LOGIN) {
                authViewModel.signIn(email, password)
            } else {
                val displayName = binding.displayNameEdit.text?.toString().orEmpty()
                authViewModel.signUp(email = email, password = password, displayName = displayName)
            }
        }

        binding.secondaryButton.setOnClickListener {
            mode = if (mode == Mode.LOGIN) Mode.SIGN_UP else Mode.LOGIN
            authViewModel.clearError()
            animateModeSwitch { render() }
        }

        binding.forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun wireValidation() {
        if (validationWired) {
            updateValidationState()
            return
        }
        validationWired = true

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                updateValidationState()
            }
        }

        binding.emailEdit.addTextChangedListener(watcher)
        binding.passwordEdit.addTextChangedListener(watcher)
        binding.displayNameEdit.addTextChangedListener(watcher)

        updateValidationState()
    }

    private fun updateValidationState() {
        val email = binding.emailEdit.text?.toString().orEmpty().trim()
        val password = binding.passwordEdit.text?.toString().orEmpty()
        val name = binding.displayNameEdit.text?.toString().orEmpty().trim()
        val emailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val baseValid = emailValid && password.isNotBlank()
        val formValid = if (mode == Mode.SIGN_UP) {
            baseValid && password.length >= 6 && name.isNotBlank()
        } else {
            baseValid
        }

        val loading = authViewModel.uiStateLiveData.value?.isLoading == true
        binding.primaryButton.isEnabled = formValid && !loading
        binding.primaryButton.alpha = if (binding.primaryButton.isEnabled) 1f else 0.55f

        if (emailValid) {
            binding.emailLayout.error = null
        }
        if (password.isNotBlank()) {
            binding.passwordLayout.error = null
        }
    }

    private fun observe() {
        authViewModel.uiStateLiveData.observe(this) { state ->
            binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            binding.emailLayout.isEnabled = !state.isLoading
            binding.passwordLayout.isEnabled = !state.isLoading
            binding.secondaryButton.isEnabled = !state.isLoading
            binding.displayNameLayout.isEnabled = !state.isLoading

            val msg = mapAuthError(state.errorMessage)
            binding.errorText.text = msg.orEmpty()
            setVisibleAnimated(binding.errorText, !msg.isNullOrBlank())

            if (!msg.isNullOrBlank() && msg != lastErrorMessage) {
                lastErrorMessage = msg
                showErrorFeedback()
            }

            updateValidationState()

            val sentTo = state.verificationEmailSentTo
            if (!sentTo.isNullOrBlank() && sentTo != lastVerificationSentTo) {
                lastVerificationSentTo = sentTo
                showVerificationSentDialog(sentTo)
                authViewModel.clearVerificationEmailSent()
            }

            if (state.needsEmailVerification && !showingNeedsVerificationDialog) {
                showingNeedsVerificationDialog = true
                showEmailNotVerifiedDialog(state.email)
                authViewModel.clearNeedsEmailVerification()
            }

            if (state.isAuthenticated && !hasNavigated) {
                hasNavigated = true
                playSuccessSequence(state.displayName, state.email)
            }
        }
    }

    private fun showVerificationSentDialog(email: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Verification email sent")
            .setMessage("Verification email sent to $email. Please verify to continue.")
            .setPositiveButton("Go to Login") { _, _ ->
                mode = Mode.LOGIN
                authViewModel.clearError()
                render()
            }
            .show()
    }

    private fun showEmailNotVerifiedDialog(email: String?) {
        val shownEmail = email?.takeIf { it.isNotBlank() } ?: "your email"
        MaterialAlertDialogBuilder(this)
            .setTitle("Email not verified")
            .setMessage("Email not verified. Please check your inbox for $shownEmail.")
            .setNegativeButton("OK") { _, _ ->
                showingNeedsVerificationDialog = false
                authViewModel.clearError()
            }
            .setPositiveButton("Resend Link") { _, _ ->
                showingNeedsVerificationDialog = false
                authViewModel.resendVerificationEmail()
            }
            .setOnCancelListener {
                showingNeedsVerificationDialog = false
                authViewModel.clearError()
            }
            .show()
    }

    private fun showForgotPasswordDialog() {
        val ctx = this
        val emailLayout = TextInputLayout(ctx).apply {
            hint = "Email"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            val r = resources.displayMetrics.density * 14f
            setBoxCornerRadii(r, r, r, r)
        }
        val emailEdit = TextInputEditText(ctx).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.emailEdit.text?.toString().orEmpty())
        }
        emailLayout.addView(emailEdit)

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Reset password")
            .setMessage("Enter your email and we'll send you a reset link.")
            .setView(emailLayout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send") { _, _ ->
                val email = emailEdit.text?.toString().orEmpty()
                authViewModel.sendPasswordReset(email) { result ->
                    result.onSuccess {
                        runOnUiThread {
                            Toast.makeText(ctx, "Reset link sent", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun showErrorFeedback() {
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        binding.glassCardContent.startAnimation(shake)

        val errColor = MaterialColors.getColor(binding.emailLayout, com.google.android.material.R.attr.colorError)
        binding.emailLayout.boxStrokeColor = errColor
        binding.passwordLayout.boxStrokeColor = errColor
        binding.emailLayout.error = " "
        binding.passwordLayout.error = " "

        binding.content.postDelayed({
            val normal = MaterialColors.getColor(binding.emailLayout, com.google.android.material.R.attr.colorOutline)
            binding.emailLayout.boxStrokeColor = normal
            binding.passwordLayout.boxStrokeColor = normal
            binding.emailLayout.error = null
            binding.passwordLayout.error = null
        }, 900)
    }

    private fun playSuccessSequence(displayName: String?, email: String?) {
        binding.primaryButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        val name = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "friend"

        binding.greetingText.text = "Welcome to the moment, ${name}."
        setVisibleAnimated(binding.greetingText, true)

        val startWidth = binding.primaryButton.width
        val targetSize = binding.primaryButton.height
        binding.primaryButton.isEnabled = false
        binding.secondaryButton.isEnabled = false
        binding.emailLayout.isEnabled = false
        binding.passwordLayout.isEnabled = false
        binding.displayNameLayout.isEnabled = false
        binding.forgotPassword.isEnabled = false

        binding.primaryButton.text = ""
        binding.primaryButton.icon = getDrawable(R.drawable.ic_check)
        binding.primaryButton.iconTint = getColorStateList(android.R.color.white)
        binding.primaryButton.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
        binding.primaryButton.iconSize = (resources.displayMetrics.density * 22).toInt()
        binding.primaryButton.iconPadding = 0
        binding.primaryButton.setBackgroundTintList(getColorStateList(R.color.sage_primary))

        val animator = android.animation.ValueAnimator.ofInt(startWidth, targetSize)
        animator.duration = 360
        animator.interpolator = FastOutSlowInInterpolator()
        animator.addUpdateListener { va ->
            val w = va.animatedValue as Int
            val lp = binding.primaryButton.layoutParams
            lp.width = w
            binding.primaryButton.layoutParams = lp
            binding.primaryButton.cornerRadius = targetSize / 2
        }
        animator.start()

        binding.primaryButton.postDelayed({
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val opts = ActivityOptions.makeCustomAnimation(
                this,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            startActivity(intent, opts.toBundle())
            finish()
        }, 1500)
    }

    private fun animateModeSwitch(applyChanges: () -> Unit) {
        val dy = (resources.displayMetrics.density * 6f)
        val views = listOf(
            binding.heading,
            binding.subtitle,
            binding.benefits,
            binding.emailLayout,
            binding.passwordLayout,
            binding.forgotPassword,
            binding.passwordHint,
            binding.primaryButton,
            binding.secondaryButton,
            binding.termsText
        )

        // Fade slightly out
        views.forEach { v ->
            v.animate().cancel()
            v.animate()
                .alpha(0.0f)
                .translationY(dy)
                .setDuration(110)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }

        // Apply text/visibility changes mid-transition
        binding.content.postDelayed({
            applyChanges()
            views.forEach { v ->
                if (v.visibility == View.GONE) return@forEach
                v.alpha = 0f
                v.translationY = dy
                v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            }
        }, 120)
    }

    private fun setVisibleAnimated(view: View, visible: Boolean) {
        if (visible) {
            if (view.visibility == View.VISIBLE && view.alpha == 1f) return
            view.animate().cancel()
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.translationY = (resources.displayMetrics.density * 6f)
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        } else {
            if (view.visibility != View.VISIBLE) return
            view.animate().cancel()
            view.animate()
                .alpha(0f)
                .translationY((resources.displayMetrics.density * 6f))
                .setDuration(140)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction {
                    view.visibility = View.GONE
                    view.alpha = 1f
                    view.translationY = 0f
                }
                .start()
        }
    }

    private fun render() {
        if (mode == Mode.LOGIN) {
            binding.heading.text = "Welcome Back"
            binding.subtitle.text = "Welcome back. Sign in to continue."
            binding.primaryButton.text = "Login"
            binding.secondaryButton.text = "No account? Sign Up"
            setVisibleAnimated(binding.forgotPassword, true)
            setVisibleAnimated(binding.passwordHint, false)
            setVisibleAnimated(binding.displayNameLayout, false)
            setVisibleAnimated(binding.greetingText, false)
        } else {
            binding.heading.text = "Create your space"
            binding.subtitle.text = "Create an account to save your progress."
            binding.primaryButton.text = "Create Account"
            binding.secondaryButton.text = "Have an account? Login"
            setVisibleAnimated(binding.forgotPassword, false)
            setVisibleAnimated(binding.passwordHint, true)
            setVisibleAnimated(binding.displayNameLayout, true)
            setVisibleAnimated(binding.greetingText, false)
        }

        updateValidationState()
    }

    private fun mapAuthError(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        val normalized = raw.uppercase()
        if (normalized.contains("CONFIGURATION_NOT_FOUND")) {
            return "Firebase configuration not found. Fix: In Firebase Console -> Authentication -> Sign-in method, enable Email/Password. Also ensure this app (package com.example.ourmajor) is added in Firebase and you downloaded the latest google-services.json, then Sync/Rebuild."
        }

        return raw
    }
}
