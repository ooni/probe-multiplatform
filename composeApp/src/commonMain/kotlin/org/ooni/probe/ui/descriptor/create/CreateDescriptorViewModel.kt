package org.ooni.probe.ui.descriptor.create

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.ooni.engine.models.Failure
import org.ooni.engine.models.OONIRunLinkCreateRequest
import org.ooni.engine.models.OoniAuthLoginResponse
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.domain.auth.AuthException
import org.ooni.probe.domain.descriptors.OonirunApiError
import org.ooni.probe.domain.descriptors.SaveTestDescriptors
import org.ooni.probe.shared.now
import org.ooni.probe.shared.today
import org.ooni.probe.ui.shared.isValidUrl

class CreateDescriptorViewModel(
    loginToken: String?,
    onBack: () -> Unit,
    private val onDescriptorCreated: (Descriptor.Id) -> Unit,
    private val getStoredSession: suspend () -> AuthSession?,
    private val requestLogin: suspend (String) -> Result<OoniAuthLoginResponse, AuthException>,
    private val exchangeLoginToken: suspend (String) -> Result<AuthSession, AuthException>,
    private val clearSession: suspend () -> Unit,
    private val createDescriptor: suspend (OONIRunLinkCreateRequest) -> Result<Descriptor, OonirunApiError>,
    private val saveTestDescriptors: suspend (List<Descriptor>, SaveTestDescriptors.Mode) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = getStoredSession()
            when {
                session != null -> _state.value = State.LoggedIn(session = session)

                loginToken != null -> {
                    _state.value = State.AwaitingToken(emailAddress = "", tokenInput = loginToken)
                    verifyToken()
                }

                else -> _state.value = State.LoggedOut()
            }
        }

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.EmailChanged>()
            .onEach { event ->
                updateLoggedOut { it.copy(emailAddress = event.value, errorMessage = null) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SendLoginLinkClicked>()
            .onEach { sendLoginLink() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.TokenChanged>()
            .onEach { event ->
                updateAwaitingToken { it.copy(tokenInput = event.value, errorMessage = null) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.VerifyClicked>()
            .onEach { verifyToken() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.LogOutClicked>()
            .onEach {
                clearSession()
                _state.value = State.LoggedOut()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.NameChanged>()
            .onEach { event -> updateLoggedIn { it.copy(name = event.value, errorMessage = null) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ShortDescriptionChanged>()
            .onEach { event ->
                updateLoggedIn { it.copy(shortDescription = event.value, errorMessage = null) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DescriptionChanged>()
            .onEach { event ->
                updateLoggedIn { it.copy(description = event.value, errorMessage = null) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.IconChanged>()
            .onEach { event -> updateLoggedIn { it.copy(icon = event.value, errorMessage = null) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ExpirationDateChanged>()
            .onEach { event ->
                updateLoggedIn { it.copy(expirationDate = event.value, errorMessage = null) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UrlChanged>()
            .onEach { event ->
                updateLoggedIn { state ->
                    state.copy(
                        errorMessage = null,
                        urls = state.urls.mapIndexed { index, item ->
                            if (index == event.index) item.copy(url = event.url, hasError = false) else item
                        },
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AddUrlClicked>()
            .onEach {
                updateLoggedIn { state ->
                    if (!state.canAddUrls) return@updateLoggedIn state
                    state.copy(urls = state.urls + UrlItem(), errorMessage = null)
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DeleteUrlClicked>()
            .onEach { event ->
                updateLoggedIn { state ->
                    if (!state.canRemoveUrls) return@updateLoggedIn state
                    state.copy(
                        errorMessage = null,
                        urls = state.urls.filterIndexed { index, _ -> index != event.index },
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SubmitClicked>()
            .onEach { submit() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun sendLoginLink() {
        val current = _state.value as? State.LoggedOut ?: return
        val emailAddress = current.emailAddress.trim()
        if (!emailAddress.isValidEmailAddress()) {
            updateLoggedOut { it.copy(errorMessage = ErrorMessage.EmailInvalid) }
            return
        }

        updateLoggedOut { it.copy(isBusy = true, errorMessage = null) }
        when (val result = requestLogin(emailAddress)) {
            is Success -> _state.value = State.AwaitingToken(emailAddress = emailAddress)

            is Failure ->
                updateLoggedOut { it.copy(isBusy = false, errorMessage = result.reason.toErrorMessage()) }
        }
    }

    private suspend fun verifyToken() {
        val current = _state.value as? State.AwaitingToken ?: return
        val token = current.tokenInput.extractLoginToken()
        if (token.isEmpty()) {
            updateAwaitingToken { it.copy(errorMessage = ErrorMessage.TokenEmpty) }
            return
        }

        updateAwaitingToken { it.copy(isBusy = true, errorMessage = null) }
        when (val result = exchangeLoginToken(token)) {
            is Success -> _state.value = State.LoggedIn(session = result.value)

            is Failure ->
                updateAwaitingToken { it.copy(isBusy = false, errorMessage = result.reason.toErrorMessage()) }
        }
    }

    private suspend fun submit() {
        val current = _state.value as? State.LoggedIn ?: return
        if (current.isSubmitting) return

        if (current.name.isBlank() || current.shortDescription.isBlank() || current.description.isBlank()) {
            updateLoggedIn { it.copy(errorMessage = ErrorMessage.FieldsRequired) }
            return
        }

        val urls = current.urls.map { it.copy(hasError = !it.url.isValidUrl()) }
        if (urls.any { it.hasError }) {
            updateLoggedIn { it.copy(urls = urls, errorMessage = ErrorMessage.InvalidUrls) }
            return
        }

        updateLoggedIn { it.copy(isSubmitting = true, errorMessage = null) }

        val request = OONIRunLinkCreateRequest(
            name = current.name.trim(),
            shortDescription = current.shortDescription.trim(),
            description = current.description.trim(),
            author = current.session.emailAddress,
            icon = current.icon,
            expirationDate = current.expirationDate.atStartOfDayIn(TimeZone.UTC),
            nettests = listOf(
                NetTest(
                    test = TestType.WebConnectivity,
                    inputs = urls.map { it.url }.distinct(),
                    options = null,
                ).toOONI(),
            ),
        )

        when (val result = createDescriptor(request)) {
            is Success -> {
                saveTestDescriptors(
                    listOf(result.value.copy(autoUpdate = true, dateInstalled = LocalDateTime.now())),
                    SaveTestDescriptors.Mode.CreateOrUpdate,
                )
                updateLoggedIn { it.copy(isSubmitting = false) }
                onDescriptorCreated(result.value.id)
            }

            is Failure ->
                updateLoggedIn {
                    it.copy(isSubmitting = false, errorMessage = result.reason.toErrorMessage())
                }
        }
    }

    private fun updateLoggedOut(transform: (State.LoggedOut) -> State.LoggedOut) {
        _state.update { if (it is State.LoggedOut) transform(it) else it }
    }

    private fun updateAwaitingToken(transform: (State.AwaitingToken) -> State.AwaitingToken) {
        _state.update { if (it is State.AwaitingToken) transform(it) else it }
    }

    private fun updateLoggedIn(transform: (State.LoggedIn) -> State.LoggedIn) {
        _state.update { if (it is State.LoggedIn) transform(it) else it }
    }

    private fun String.isValidEmailAddress(): Boolean {
        val atIndex = indexOf('@')
        return atIndex > 0 && indexOf('.', atIndex) > atIndex + 1 && !endsWith('.') && none { it.isWhitespace() }
    }

    /**
     * The user can paste either the whole emailed link or just the token inside it.
     */
    private fun String.extractLoginToken(): String {
        val value = trim()
        if (!value.startsWith("http://") && !value.startsWith("https://")) return value
        return value
            .substringAfter('?', "")
            .substringBefore('#')
            .split('&')
            .firstOrNull { it.startsWith("$TOKEN_PARAMETER=") }
            ?.substringAfter("$TOKEN_PARAMETER=")
            .orEmpty()
    }

    private fun AuthException.toErrorMessage() =
        when (this) {
            is AuthException.Http -> ErrorMessage.LoginFailed
            is AuthException.Network -> ErrorMessage.Network
            is AuthException.Decode -> ErrorMessage.Unexpected
            is AuthException.NotLoggedIn -> ErrorMessage.NotLoggedIn
        }

    private fun OonirunApiError.toErrorMessage() =
        when (this) {
            is OonirunApiError.InvalidNetTests -> ErrorMessage.InvalidNetTests
            is OonirunApiError.BadRequest,
            is OonirunApiError.Forbidden,
            is OonirunApiError.ValidationErrors,
            -> ErrorMessage.Rejected

            is OonirunApiError.Unauthorized -> ErrorMessage.NotLoggedIn
            OonirunApiError.RateLimited -> ErrorMessage.RateLimited
            is OonirunApiError.Network -> ErrorMessage.Network
            OonirunApiError.NotFound,
            is OonirunApiError.Decode,
            is OonirunApiError.Unknown,
            -> ErrorMessage.Unexpected
        }

    data class UrlItem(
        val url: String = "https://",
        val hasError: Boolean = false,
    )

    sealed interface State {
        data object Loading : State

        data class LoggedOut(
            val emailAddress: String = "",
            val isBusy: Boolean = false,
            val errorMessage: ErrorMessage? = null,
        ) : State

        data class AwaitingToken(
            val emailAddress: String,
            val tokenInput: String = "",
            val isBusy: Boolean = false,
            val errorMessage: ErrorMessage? = null,
        ) : State

        data class LoggedIn(
            val session: AuthSession,
            val name: String = "",
            val shortDescription: String = "",
            val description: String = "",
            val icon: String? = null,
            val urls: List<UrlItem> = listOf(UrlItem()),
            val expirationDate: LocalDate = defaultExpirationDate(),
            val isSubmitting: Boolean = false,
            val errorMessage: ErrorMessage? = null,
        ) : State {
            val canAddUrls get() = urls.size < MAX_URLS
            val canRemoveUrls get() = urls.size > 1
        }
    }

    enum class ErrorMessage {
        EmailInvalid,
        TokenEmpty,
        LoginFailed,
        NotLoggedIn,
        Network,
        RateLimited,
        FieldsRequired,
        InvalidUrls,
        InvalidNetTests,
        Rejected,
        Unexpected,
    }

    sealed interface Event {
        data object BackClicked : Event

        data class EmailChanged(
            val value: String,
        ) : Event

        data object SendLoginLinkClicked : Event

        data class TokenChanged(
            val value: String,
        ) : Event

        data object VerifyClicked : Event

        data object LogOutClicked : Event

        data class NameChanged(
            val value: String,
        ) : Event

        data class ShortDescriptionChanged(
            val value: String,
        ) : Event

        data class DescriptionChanged(
            val value: String,
        ) : Event

        data class IconChanged(
            val value: String?,
        ) : Event

        data class ExpirationDateChanged(
            val value: LocalDate,
        ) : Event

        data class UrlChanged(
            val index: Int,
            val url: String,
        ) : Event

        data object AddUrlClicked : Event

        data class DeleteUrlClicked(
            val index: Int,
        ) : Event

        data object SubmitClicked : Event
    }

    companion object {
        private const val TOKEN_PARAMETER = "token"

        @VisibleForTesting
        const val MAX_URLS = 100

        /**
         * The server would otherwise default to 6 months, which is too short for a link people
         * are expected to keep sharing.
         */
        fun defaultExpirationDate() = LocalDate.today().plus(5, DateTimeUnit.YEAR)
    }
}
