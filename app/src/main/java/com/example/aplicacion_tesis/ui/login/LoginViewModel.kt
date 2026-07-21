package com.example.aplicacion_tesis.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplicacion_tesis.network.RetrofitClient
import com.example.aplicacion_tesis.network.TokenStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.example.aplicacion_tesis.model.dto.AuthResponse
import com.example.aplicacion_tesis.model.dto.LoginRequest

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(correo: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val resp = RetrofitClient.authApi.login(
                    LoginRequest(
                        correo = correo.trim(),
                        password = password.trim()   // 👈 importante
                    )
                )

                val user = resp.user        // viene de "data" en el JSON
                val token = resp.token

                val idEstudiante = user?.id_estudiante
                val idDocente = user?.id_docente

                if (resp.status && !token.isNullOrBlank() && user != null) {
                    val fullName = "${user.nombre} ${user.apellidos}".trim()

                    TokenStore.save(
                        tokenValue = token,
                        userIdValue = user.id_usuario,
                        roleValue = user.rol,
                        nameValue = fullName,
                        studentIdValue = idEstudiante,
                        teacherIdValue = idDocente,
                        emailValue = user.correo
                    )

                    _state.value = LoginState.Success(token)
                } else {
                    _state.value = LoginState.Error(
                        resp.message ?: "Credenciales inválidas."
                    )
                }

            } catch (e: HttpException) {
                // El servidor SÍ manda un mensaje claro en el body ("Correo o
                // contraseña incorrectos.", "Faltan campos"...), pero Retrofit
                // trata cualquier 4xx/5xx como excepción y no lo entrega solo:
                // antes esto mostraba "Error del servidor (401)" — un código
                // que no le dice nada al usuario. Ahora se lee ese mensaje.
                val mensajeServidor = try {
                    e.response()?.errorBody()?.string()
                        ?.let { Gson().fromJson(it, AuthResponse::class.java) }
                        ?.message
                } catch (parseErr: Exception) {
                    null
                }
                _state.value = LoginState.Error(
                    mensajeServidor ?: "No se pudo iniciar sesión. Vuelve a intentarlo."
                )
            } catch (e: java.net.SocketTimeoutException) {
                // Railway "despierta" el servidor tras inactividad: el primer
                // intento del día puede tardar más de lo normal.
                _state.value = LoginState.Error(
                    "El servidor está tardando en responder ⏳ Espera unos segundos y vuelve a intentar."
                )
            } catch (e: IOException) {
                _state.value = LoginState.Error("Sin conexión a internet 😕 Revisa el WiFi o tus datos.")
            } catch (e: Exception) {
                _state.value = LoginState.Error("Ocurrió un problema al iniciar sesión. Vuelve a intentarlo.")
            }
        }
    }
}

