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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.ooni.probe.domain.auth.AuthException
import org.ooni.probe.domain.descriptors.OonirunApiError
import org.ooni.probe.domain.descriptors.SaveTestDescriptors
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
            assertIs<CreateDescriptorViewModel.State.LoggedIn>(state)
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
            val state = viewModel.state.first()
            assertIs<CreateDescriptorViewModel.State.AwaitingToken>(state)
            assertEquals("a@example.org", state.emailAddress)
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
            assertIs<CreateDescriptorViewModel.State.LoggedIn>(state)
            assertEquals(session, state.session)
        }

    @Test
    fun invalidUrlBlocksSubmission() =
        runTest(dispatcher) {
            var created = false
            val viewModel = buildViewModel(
                createDescriptor = {
                    created = true
                    error("should not be called")
                },
            )

            fillInForm(viewModel)
            viewModel.onEvent(CreateDescriptorViewModel.Event.UrlChanged(0, "not-a-url"))
            viewModel.onEvent(CreateDescriptorViewModel.Event.SubmitClicked)

            val state = viewModel.state.first()
            assertTrue(!created)
            assertIs<CreateDescriptorViewModel.State.LoggedIn>(state)
            assertEquals(CreateDescriptorViewModel.ErrorMessage.InvalidUrls, state.errorMessage)
            assertTrue(state.urls.single().hasError)
        }

    @Test
    fun aBlankUrlRowBlocksSubmission() =
        runTest(dispatcher) {
            var created = false
            val viewModel = buildViewModel(
                createDescriptor = {
                    created = true
                    error("should not be called")
                },
            )

            fillInForm(viewModel)
            viewModel.onEvent(CreateDescriptorViewModel.Event.AddUrlClicked)
            viewModel.onEvent(CreateDescriptorViewModel.Event.UrlChanged(1, ""))
            viewModel.onEvent(CreateDescriptorViewModel.Event.SubmitClicked)

            val state = viewModel.state.first()
            assertTrue(!created)
            assertIs<CreateDescriptorViewModel.State.LoggedIn>(state)
            assertEquals(CreateDescriptorViewModel.ErrorMessage.InvalidUrls, state.errorMessage)
            assertTrue(state.urls[1].hasError)
        }

    @Test
    fun urlRowsCanBeAddedAndRemoved() =
        runTest(dispatcher) {
            val viewModel = buildViewModel()

            viewModel.onEvent(CreateDescriptorViewModel.Event.AddUrlClicked)
            viewModel.onEvent(CreateDescriptorViewModel.Event.UrlChanged(1, "https://example.org"))

            var state = viewModel.state.first()
            assertIs<CreateDescriptorViewModel.State.LoggedIn>(state)
            assertEquals(2, state.urls.size)

            viewModel.onEvent(CreateDescriptorViewModel.Event.DeleteUrlClicked(0))

            state = viewModel.state.first()
            assertIs<CreateDescriptorViewModel.State.LoggedIn>(state)
            assertEquals(listOf("https://example.org"), state.urls.map { it.url })

            // The last remaining row cannot be removed.
            viewModel.onEvent(CreateDescriptorViewModel.Event.DeleteUrlClicked(0))

            state = viewModel.state.first()
            assertIs<CreateDescriptorViewModel.State.LoggedIn>(state)
            assertEquals(1, state.urls.size)
        }

    @Test
    fun aLoginTokenFromADeepLinkIsVerifiedOnStart() =
        runTest(dispatcher) {
            var exchangedToken: String? = null
            val viewModel = buildViewModel(
                loginToken = "deep-link-token",
                storedSession = { null },
                exchangeLoginToken = { token ->
                    exchangedToken = token
                    Success(session)
                },
            )

            assertEquals("deep-link-token", exchangedToken)
            val state = viewModel.state.first()
            assertIs<CreateDescriptorViewModel.State.LoggedIn>(state)
            assertEquals(session, state.session)
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
            assertIs<CreateDescriptorViewModel.State.LoggedOut>(state)
        }

    @Test
    fun successfulCreateNavigatesToTheNewDescriptor() =
        runTest(dispatcher) {
            var request: OONIRunLinkCreateRequest? = null
            var createdId: Descriptor.Id? = null
            val viewModel = buildViewModel(
                onDescriptorCreated = { createdId = it },
                createDescriptor = {
                    request = it
                    Success(descriptor("1234"))
                },
            )

            fillInForm(viewModel)
            viewModel.onEvent(CreateDescriptorViewModel.Event.SubmitClicked)

            assertEquals(Descriptor.Id("1234"), createdId)
            assertEquals("a@example.org", request?.author)
            assertEquals("My link", request?.name)
            assertEquals(listOf("https://ooni.org"), request?.nettests?.single()?.inputs)
            assertEquals("web_connectivity", request?.nettests?.single()?.name)
            assertEquals(
                CreateDescriptorViewModel.defaultExpirationDate(),
                request?.expirationDate?.toLocalDateTime(TimeZone.UTC)?.date,
            )
        }

    @Test
    fun selectedIconIsIncludedInTheRequest() =
        runTest(dispatcher) {
            var request: OONIRunLinkCreateRequest? = null
            val viewModel = buildViewModel(
                createDescriptor = {
                    request = it
                    Success(descriptor("1234"))
                },
            )

            fillInForm(viewModel)
            viewModel.onEvent(CreateDescriptorViewModel.Event.IconChanged("FaRocket"))
            viewModel.onEvent(CreateDescriptorViewModel.Event.SubmitClicked)

            assertEquals("FaRocket", request?.icon)
        }

    @Test
    fun submissionSucceedsWithNoIconSelected() =
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

            assertNull(request?.icon)
        }

    @Test
    fun aCreatedDescriptorIsInstalledLocallyBeforeNavigating() =
        runTest(dispatcher) {
            var installed: List<Descriptor>? = null
            var installMode: SaveTestDescriptors.Mode? = null
            var createdId: Descriptor.Id? = null
            val viewModel = buildViewModel(
                onDescriptorCreated = { createdId = it },
                createDescriptor = { Success(descriptor("1234")) },
                saveTestDescriptors = { models, mode ->
                    installed = models
                    installMode = mode
                    // Must happen before we navigate to the descriptor screen.
                    assertNull(createdId)
                },
            )

            fillInForm(viewModel)
            viewModel.onEvent(CreateDescriptorViewModel.Event.SubmitClicked)

            assertEquals(Descriptor.Id("1234"), installed?.single()?.id)
            assertNotNull(installed?.single()?.dateInstalled)
            assertEquals(SaveTestDescriptors.Mode.CreateOrUpdate, installMode)
            assertEquals(Descriptor.Id("1234"), createdId)
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
            assertIs<CreateDescriptorViewModel.State.LoggedIn>(state)
            assertEquals(CreateDescriptorViewModel.ErrorMessage.RateLimited, state.errorMessage)
        }

    private fun fillInForm(viewModel: CreateDescriptorViewModel) {
        viewModel.onEvent(CreateDescriptorViewModel.Event.NameChanged("My link"))
        viewModel.onEvent(CreateDescriptorViewModel.Event.ShortDescriptionChanged("Short"))
        viewModel.onEvent(CreateDescriptorViewModel.Event.DescriptionChanged("Long description"))
        viewModel.onEvent(CreateDescriptorViewModel.Event.UrlChanged(0, "https://ooni.org"))
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
        loginToken: String? = null,
        onBack: () -> Unit = {},
        onDescriptorCreated: (Descriptor.Id) -> Unit = {},
        saveTestDescriptors: suspend (List<Descriptor>, SaveTestDescriptors.Mode) -> Unit = { _, _ -> },
        storedSession: suspend () -> AuthSession? = { session },
        requestLogin: suspend (String) -> Result<OoniAuthLoginResponse, AuthException> = {
            Failure(AuthException.NotLoggedIn())
        },
        exchangeLoginToken: suspend (String) -> Result<AuthSession, AuthException> = {
            Failure(AuthException.NotLoggedIn())
        },
        clearSession: suspend () -> Unit = {},
        createDescriptor: suspend (OONIRunLinkCreateRequest) -> Result<Descriptor, OonirunApiError> = {
            Failure(OonirunApiError.NotFound)
        },
    ) = CreateDescriptorViewModel(
        loginToken = loginToken,
        onBack = onBack,
        onDescriptorCreated = onDescriptorCreated,
        getStoredSession = storedSession,
        requestLogin = requestLogin,
        exchangeLoginToken = exchangeLoginToken,
        clearSession = clearSession,
        createDescriptor = createDescriptor,
        saveTestDescriptors = saveTestDescriptors,
    )
}
