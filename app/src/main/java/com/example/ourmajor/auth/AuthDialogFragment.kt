package com.example.ourmajor.auth

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.ourmajor.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AuthDialogFragment : DialogFragment() {

    private lateinit var viewModel: AuthViewModel

    private var mode: Mode = Mode.LOGIN

    private lateinit var emailLayout: TextInputLayout
    private lateinit var emailEdit: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var passwordEdit: TextInputEditText
    private lateinit var errorText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var primaryButton: MaterialButton
    private lateinit var secondaryButton: MaterialButton

    private var lastVerificationSentTo: String? = null
    private var showingNeedsVerificationDialog: Boolean = false

    enum class Mode { LOGIN, SIGN_UP }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val space = dp(12)
        val spaceSmall = dp(8)

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        val title = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
        }

        val subtitle = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, spaceSmall, 0, space)
        }

        emailLayout = TextInputLayout(ctx).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            val r = dp(14).toFloat()
            setBoxCornerRadii(r, r, r, r)
        }
        emailEdit = TextInputEditText(ctx).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        emailLayout.addView(emailEdit)
        emailLayout.hint = "Email"
        emailLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = spaceSmall
        }

        passwordLayout = TextInputLayout(ctx).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            val r = dp(14).toFloat()
            setBoxCornerRadii(r, r, r, r)
        }
        passwordEdit = TextInputEditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        passwordLayout.addView(passwordEdit)
        passwordLayout.hint = "Password"

        errorText = TextView(ctx).apply {
            setPadding(0, spaceSmall, 0, 0)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorError))
            visibility = View.GONE
        }

        progress = ProgressBar(ctx).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        val progressRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = space
            }
            addView(progress)
        }

        primaryButton = MaterialButton(ctx).apply {
            isAllCaps = false
            cornerRadius = dp(14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = space
            }
        }
        secondaryButton = MaterialButton(
            ctx,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            isAllCaps = false
            cornerRadius = dp(14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = spaceSmall
            }
        }

        container.addView(title)
        container.addView(subtitle)
        container.addView(emailLayout)
        container.addView(passwordLayout)
        container.addView(errorText)
        container.addView(primaryButton)
        container.addView(secondaryButton)
        container.addView(progressRow)

        fun render() {
            title.text = if (mode == Mode.LOGIN) "Login" else "Sign Up"
            subtitle.text =
                if (mode == Mode.LOGIN) "Welcome back" else "Create an account"
            primaryButton.text =
                if (mode == Mode.LOGIN) "Login" else "Create Account"
            secondaryButton.text =
                if (mode == Mode.LOGIN) "No account? Sign Up" else "Have an account? Login"
        }

        render()

        primaryButton.setOnClickListener {
            val email = emailEdit.text.toString()
            val pass = passwordEdit.text.toString()
            if (mode == Mode.LOGIN) viewModel.signIn(email, pass)
            else viewModel.signUp(email, pass)
        }

        secondaryButton.setOnClickListener {
            mode = if (mode == Mode.LOGIN) Mode.SIGN_UP else Mode.LOGIN
            viewModel.clearError()
            render()
        }

        viewModel.uiStateLiveData.observe(this) { state ->
            progress.isVisible = state.isLoading

            emailLayout.isEnabled = !state.isLoading
            passwordLayout.isEnabled = !state.isLoading
            primaryButton.isEnabled = !state.isLoading
            secondaryButton.isEnabled = !state.isLoading

            errorText.text = state.errorMessage.orEmpty()
            errorText.isVisible = !state.errorMessage.isNullOrBlank()

            val sentTo = state.verificationEmailSentTo
            if (!sentTo.isNullOrBlank() && sentTo != lastVerificationSentTo) {
                lastVerificationSentTo = sentTo
                Toast.makeText(ctx, "Verification email sent to $sentTo. Please verify to continue.", Toast.LENGTH_LONG).show()
                mode = Mode.LOGIN
                viewModel.clearVerificationEmailSent()
                render()
            }

            if (state.needsEmailVerification && !showingNeedsVerificationDialog) {
                showingNeedsVerificationDialog = true
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("Email not verified")
                    .setMessage("Email not verified. Please check your inbox.")
                    .setNegativeButton("OK") { _, _ ->
                        showingNeedsVerificationDialog = false
                        viewModel.clearNeedsEmailVerification()
                    }
                    .setPositiveButton("Resend Link") { _, _ ->
                        showingNeedsVerificationDialog = false
                        viewModel.clearNeedsEmailVerification()
                        viewModel.resendVerificationEmail()
                    }
                    .setOnCancelListener {
                        showingNeedsVerificationDialog = false
                        viewModel.clearNeedsEmailVerification()
                    }
                    .show()
            }

            if (state.isAuthenticated) dismissAllowingStateLoss()
        }

        return MaterialAlertDialogBuilder(ctx)
            .setView(container)
            .create()
    }

    companion object {
        const val TAG = "auth_dialog"
    }
}
