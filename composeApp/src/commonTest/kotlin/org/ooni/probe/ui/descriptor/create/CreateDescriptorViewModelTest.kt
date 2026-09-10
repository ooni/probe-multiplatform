package org.ooni.probe.ui.descriptor.create

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.ooni.engine.models.Failure
import org.ooni.engine.models.OONIRunLinkCreateRequest
import org.ooni.engine.models.OoniAuthLoginResponse
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.di.Dependencies
import org.ooni.probe.domain.auth.AuthException
import org.ooni.probe.domain.descriptors.OonirunApiError
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class CreateDescriptorViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())

    private val session = AuthSession(
        sessionToken = "jwt-abc",
        emailAddress = "a@example.org",
        role = "user",
        loginTime = Instant.parse("2025-01-01T00:00:00Z"),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsLoggedInWhenASessionIsStored() =
        runTest(dispatcher) {
            val viewModel = buildViewModel()

            val state = viewModel.state.first()
            assertEquals(CreateDescriptorViewModel.Phase.LoggedIn, state.phase)
            assertEquals(session, state.session)
        }

    @Test
    fun requestingALoginLinkMovesToAwaitingToken() =
        runTest(dispatcher) {
            var requestedEmail: String? = null
            val viewModel = buildViewModel(
                storedSession = { null },
                requestLogin = { email ->
                    requestedEmail = email
                    Success(
                        OoniAuthLoginResponse(
                            emailAddress = email,
                            loginTokenExpiration = Instant.parse("2030-01-01T00:00:00Z"),
                        ),
                    )
                },
            )

            viewModel.onEvent(CreateDescriptorViewModel.Event.EmailChanged("a@example.org"))
            viewModel.onEvent(CreateDescriptorViewModel.Event.SendLoginLinkClicked)

            assertEquals("a@example.org", requestedEmail)
            assertEquals(
                CreateDescriptorViewModel.Phase.AwaitingToken,
                viewModel.state.first().phase,
            )
        }

    @Test
    fun verifyExtractsTheTokenFromAPastedLink() =
        runTest(dispatcher) {
            var exchangedToken: String? = null
            val viewModel = buildViewModel(
                storedSession = { null },
                requestLogin = {
                    Success(
                        OoniAuthLoginResponse(
                            emailAddress = it,
                            loginTokenExpiration = Instant.parse("2030-01-01T00:00:00Z"),
                        ),
                    )
                },
                exchangeLoginToken = { token ->
                    exchangedToken = token
                    Success(session)
                },
            )

            viewModel.onEvent(CreateDescriptorViewModel.Event.EmailChanged("a@example.org"))
            viewModel.onEvent(CreateDescriptorViewModel.Event.SendLoginLinkClicked)
            viewModel.onEvent(
                CreateDescriptorViewModel.Event.TokenChanged(
                    "https://run.ooni.org/login?token=abc123&next=%2F",
                ),
            )
            viewModel.onEvent(CreateDescriptorViewModel.Event.VerifyClicked)

            assertEquals("abc123", exchangedToken)
            val state = viewModel.state.first()
            assertEquals(CreateDescriptorViewModel.Phase.LoggedIn, state.phase)
            assertEquals(session, state.session)
        }

    @Test
    fun invalidOptionsJsonBlocksSubmission() =
        runTest(dispatcher) {
            var created = false
            val viewModel = buildViewModel(
                createDescriptor = {
                    created = true
                    error("should not be called")
                },
            )

            fillInForm(viewModel)
            viewModel.onEvent(CreateDescriptorViewModel.Event.NetTestOptionsChanged(0, "{ not json"))
            viewModel.onEvent(CreateDescriptorViewModel.Event.SubmitClicked)

            val state = viewModel.state.first()
            assertTrue(!created)
            assertEquals(CreateDescriptorViewModel.Phase.LoggedIn, state.phase)
            assertEquals(CreateDescriptorViewModel.ErrorMessage.InvalidOptions, state.errorMessage)
            assertTrue(state.netTests.single().hasInvalidOptions)
        }

    @Test
    fun submittingWhileLoggedOutIsBlocked() =
        runTest(dispatcher) {
            var created = false
            val viewModel = buildViewModel(
                storedSession = { null },
                createDescriptor = {
                    created = true
                    error("should not be called")
                },
            )

            viewModel.onEvent(CreateDescriptorViewModel.Event.SubmitClicked)

            val state = viewModel.state.first()
            assertTrue(!created)
            assertEquals(CreateDescriptorViewModel.Phase.LoggedOut, state.phase)
            assertNull(state.runLink)
        }

    @Test
    fun successfulCreateExposesTheShareLinks() =
        runTest(dispatcher) {
            var request: OONIRunLinkCreateRequest? = null
            val viewModel = buildViewModel(
                createDescriptor = {
                    request = it
                    Success(descriptor("1234"))
                },
            )

            fillInForm(viewModel)
            viewModel.onEvent(CreateDescriptorViewModel.Event.SubmitClicked)

            val state = viewModel.state.first()
            assertEquals(CreateDescriptorViewModel.Phase.Success, state.phase)
            assertEquals(descriptor("1234").runLink, state.runLink)
            assertEquals("ooni://runv2/1234", state.deepLink)
            assertEquals("a@example.org", request?.author)
            assertEquals("My link", request?.name)
            assertEquals(listOf("https://ooni.org"), request?.nettests?.single()?.inputs)
        }

    @Test
    fun apiFailureIsMappedToAUserFacingMessage() =
        runTest(dispatcher) {
            val viewModel = buildViewModel(
                createDescriptor = { Failure(OonirunApiError.RateLimited) },
            )

            fillInForm(viewModel)
            viewModel.onEvent(CreateDescriptorViewModel.Event.SubmitClicked)

            val state = viewModel.state.first()
            assertEquals(CreateDescriptorViewModel.Phase.LoggedIn, state.phase)
            assertEquals(CreateDescriptorViewModel.ErrorMessage.RateLimited, state.errorMessage)
        }

    private fun fillInForm(viewModel: CreateDescriptorViewModel) {
        viewModel.onEvent(CreateDescriptorViewModel.Event.NameChanged("My link"))
        viewModel.onEvent(CreateDescriptorViewModel.Event.ShortDescriptionChanged("Short"))
        viewModel.onEvent(CreateDescriptorViewModel.Event.DescriptionChanged("Long description"))
        viewModel.onEvent(
            CreateDescriptorViewModel.Event.NetTestInputsChanged(0, "https://ooni.org\n  \n"),
        )
    }

    private fun descriptor(id: String) =
        Descriptor(
            id = Descriptor.Id(id),
            revision = 1,
            name = "My link",
            shortDescription = "Short",
            description = "Long description",
            author = "a@example.org",
            netTests = emptyList(),
            nameIntl = null,
            shortDescriptionIntl = null,
            descriptionIntl = null,
            icon = null,
            color = null,
            animation = null,
            expirationDate = null,
            dateCreated = null,
            dateUpdated = null,
            dateInstalled = null,
            autoUpdate = false,
        )

    private fun buildViewModel(
        onBack: () -> Unit = {},
        storedSession: suspend () -> AuthSession? = { session },
        requestLogin: suspend (String) -> Result<OoniAuthLoginResponse, AuthException> = {
            Failure(AuthException.NotLoggedIn)
        },
        exchangeLoginToken: suspend (String) -> Result<AuthSession, AuthException> = {
            Failure(AuthException.NotLoggedIn)
        },
        clearSession: suspend () -> Unit = {},
        createDescriptor: suspend (OONIRunLinkCreateRequest) -> Result<Descriptor, OonirunApiError> = {
            Failure(OonirunApiError.NotFound)
        },
    ) = CreateDescriptorViewModel(
        onBack = onBack,
        getStoredSession = storedSession,
        requestLogin = requestLogin,
        exchangeLoginToken = exchangeLoginToken,
        clearSession = clearSession,
        createDescriptor = createDescriptor,
        json = Dependencies.buildJson(),
    )
}
