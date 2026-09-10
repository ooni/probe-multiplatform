package org.ooni.probe.ui.descriptor.create

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
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

class CreateDescriptorViewModel(
    onBack: () -> Unit,
    private val getStoredSession: suspend () -> AuthSession?,
    private val requestLogin: suspend (String) -> Result<OoniAuthLoginResponse, AuthException>,
    private val exchangeLoginToken: suspend (String) -> Result<AuthSession, AuthException>,
    private val clearSession: suspend () -> Unit,
    private val createDescriptor: suspend (OONIRunLinkCreateRequest) -> Result<Descriptor, OonirunApiError>,
    private val json: Json,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = getStoredSession()
            _state.update {
                if (session == null) {
                    it.copy(phase = Phase.LoggedOut)
                } else {
                    it.copy(phase = Phase.LoggedIn, session = session)
                }
            }
        }

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.EmailChanged>()
            .onEach { event ->
                _state.update { it.copy(emailAddress = event.value, errorMessage = null) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SendLoginLinkClicked>()
            .onEach { sendLoginLink() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.TokenChanged>()
            .onEach { event ->
                _state.update { it.copy(tokenInput = event.value, errorMessage = null) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.VerifyClicked>()
            .onEach { verifyToken() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.LogOutClicked>()
            .onEach {
                clearSession()
                _state.value = State(phase = Phase.LoggedOut)
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.NameChanged>()
            .onEach { event -> _state.update { it.copy(name = event.value, errorMessage = null) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ShortDescriptionChanged>()
            .onEach { event ->
                _state.update { it.copy(shortDescription = event.value, errorMessage = null) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DescriptionChanged>()
            .onEach { event ->
                _state.update { it.copy(description = event.value, errorMessage = null) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AddNetTestClicked>()
            .onEach {
                _state.update { it.copy(netTests = it.netTests + NetTestForm()) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RemoveNetTestClicked>()
            .onEach { event ->
                _state.update { state ->
                    if (state.netTests.size <= 1) return@update state
                    state.copy(
                        netTests = state.netTests.filterIndexed { index, _ -> index != event.index },
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.NetTestTypeChanged>()
            .onEach { event ->
                updateNetTest(event.index) { it.copy(test = event.test) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.NetTestInputsChanged>()
            .onEach { event ->
                updateNetTest(event.index) { it.copy(inputs = event.value) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.NetTestOptionsChanged>()
            .onEach { event ->
                updateNetTest(event.index) { it.copy(options = event.value, hasInvalidOptions = false) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SubmitClicked>()
            .onEach { submit() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CopyClicked>()
            .onEach { event -> _state.update { it.copy(copyToClipboard = event.text) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CopyShown>()
            .onEach { _state.update { it.copy(copyToClipboard = null) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun sendLoginLink() {
        val emailAddress = _state.value.emailAddress.trim()
        if (!emailAddress.isValidEmailAddress()) {
            _state.update { it.copy(errorMessage = ErrorMessage.EmailInvalid) }
            return
        }

        _state.update { it.copy(isBusy = true, errorMessage = null) }
        when (val result = requestLogin(emailAddress)) {
            is Success ->
                _state.update {
                    it.copy(phase = Phase.AwaitingToken, isBusy = false, tokenInput = "")
                }

            is Failure ->
                _state.update { it.copy(isBusy = false, errorMessage = result.reason.toErrorMessage()) }
        }
    }

    private suspend fun verifyToken() {
        val token = _state.value.tokenInput.extractLoginToken()
        if (token.isEmpty()) {
            _state.update { it.copy(errorMessage = ErrorMessage.TokenEmpty) }
            return
        }

        _state.update { it.copy(isBusy = true, errorMessage = null) }
        when (val result = exchangeLoginToken(token)) {
            is Success ->
                _state.update {
                    it.copy(phase = Phase.LoggedIn, isBusy = false, session = result.value)
                }

            is Failure ->
                _state.update { it.copy(isBusy = false, errorMessage = result.reason.toErrorMessage()) }
        }
    }

    private suspend fun submit() {
        val state = _state.value
        if (state.phase != Phase.LoggedIn) return

        val session = state.session
        if (session == null) {
            _state.update { it.copy(errorMessage = ErrorMessage.NotLoggedIn) }
            return
        }
        if (state.name.isBlank() || state.shortDescription.isBlank() || state.description.isBlank()) {
            _state.update { it.copy(errorMessage = ErrorMessage.FieldsRequired) }
            return
        }

        val parsedOptions = state.netTests.map { it.parseOptions() }
        if (parsedOptions.any { it is OptionsResult.Invalid }) {
            _state.update { current ->
                current.copy(
                    errorMessage = ErrorMessage.InvalidOptions,
                    netTests = current.netTests.mapIndexed { index, netTest ->
                        netTest.copy(hasInvalidOptions = parsedOptions[index] is OptionsResult.Invalid)
                    },
                )
            }
            return
        }

        val netTests = state.netTests.mapIndexed { index, netTest ->
            netTest.toNetTest((parsedOptions[index] as OptionsResult.Valid).value)
        }

        _state.update { it.copy(phase = Phase.Submitting, errorMessage = null) }

        val request = OONIRunLinkCreateRequest(
            name = state.name.trim(),
            shortDescription = state.shortDescription.trim(),
            description = state.description.trim(),
            author = session.emailAddress,
            nettests = netTests.map { it.toOONI() },
        )

        val result = createDescriptor(request)

        when (result) {
            is Success ->
                _state.update {
                    it.copy(
                        phase = Phase.Success,
                        runLink = result.value.runLink,
                        deepLink = "$DEEP_LINK_PREFIX${result.value.id.value}",
                    )
                }

            is Failure ->
                _state.update {
                    it.copy(phase = Phase.LoggedIn, errorMessage = result.reason.toErrorMessage())
                }
        }
    }

    private fun updateNetTest(
        index: Int,
        transform: (NetTestForm) -> NetTestForm,
    ) {
        _state.update { state ->
            state.copy(
                errorMessage = null,
                netTests = state.netTests.mapIndexed { itemIndex, netTest ->
                    if (itemIndex == index) transform(netTest) else netTest
                },
            )
        }
    }

    private fun NetTestForm.parseOptions(): OptionsResult {
        val text = options.trim()
        if (text.isEmpty()) return OptionsResult.Valid(null)
        return try {
            OptionsResult.Valid(json.parseToJsonElement(text).jsonObject)
        } catch (e: Exception) {
            OptionsResult.Invalid
        }
    }

    private fun NetTestForm.toNetTest(options: JsonObject?) =
        NetTest(
            test = test,
            inputs = inputs.lines().map { it.trim() }.filter { it.isNotEmpty() },
            options = options,
        )

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
            AuthException.NotLoggedIn -> ErrorMessage.NotLoggedIn
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

    private sealed interface OptionsResult {
        data class Valid(
            val value: JsonObject?,
        ) : OptionsResult

        data object Invalid : OptionsResult
    }

    data class State(
        val phase: Phase = Phase.Loading,
        val emailAddress: String = "",
        val tokenInput: String = "",
        val session: AuthSession? = null,
        val name: String = "",
        val shortDescription: String = "",
        val description: String = "",
        val netTests: List<NetTestForm> = listOf(NetTestForm()),
        val isBusy: Boolean = false,
        val errorMessage: ErrorMessage? = null,
        val runLink: String? = null,
        val deepLink: String? = null,
        val copyToClipboard: String? = null,
    ) {
        val canRemoveNetTests get() = netTests.size > 1
    }

    data class NetTestForm(
        val test: TestType = TestType.WebConnectivity,
        val inputs: String = "",
        val options: String = "",
        val hasInvalidOptions: Boolean = false,
    )

    enum class Phase {
        Loading,
        LoggedOut,
        AwaitingToken,
        LoggedIn,
        Submitting,
        Success,
    }

    enum class ErrorMessage {
        EmailInvalid,
        TokenEmpty,
        LoginFailed,
        NotLoggedIn,
        Network,
        RateLimited,
        FieldsRequired,
        InvalidOptions,
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

        data object AddNetTestClicked : Event

        data class RemoveNetTestClicked(
            val index: Int,
        ) : Event

        data class NetTestTypeChanged(
            val index: Int,
            val test: TestType,
        ) : Event

        data class NetTestInputsChanged(
            val index: Int,
            val value: String,
        ) : Event

        data class NetTestOptionsChanged(
            val index: Int,
            val value: String,
        ) : Event

        data object SubmitClicked : Event

        data class CopyClicked(
            val text: String,
        ) : Event

        data object CopyShown : Event
    }

    companion object {
        private const val TOKEN_PARAMETER = "token"
        private const val DEEP_LINK_PREFIX = "ooni://runv2/"
    }
}
