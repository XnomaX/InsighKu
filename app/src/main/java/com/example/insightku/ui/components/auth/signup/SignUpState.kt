package com.example.insightku.ui.components.auth.signup

data class SignUpState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
) {
    val isSignUpEnabled: Boolean
        get() = name.isNotBlank()
            && email.isNotBlank()
            && password.isNotBlank()
            && confirmPassword.isNotBlank()
            && nameError == null
            && emailError == null
            && passwordError == null
            && confirmPasswordError == null
}
