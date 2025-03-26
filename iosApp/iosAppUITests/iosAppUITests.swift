import XCTest

class iosAppUITests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    @MainActor
    func testOnboarding() throws {
        // UI tests must launch the application that they test.
        let app = XCUIApplication()

        setupSnapshot(app)
        app.launch()

        sleep(2)

        snapshot("00-what-is-ooni-probe")

        app.buttons.firstMatch.tap()

        snapshot("01-things-to-know")

        app.buttons.firstMatch.tap()

        sleep(1)
        app.buttons["Quiz-True"].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()

        sleep(1)
        app.buttons["Quiz-True"].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()

        sleep(1)
        snapshot("02-automated-testing")

        app.buttons["No-AutoTest"].firstMatch.tap()

        snapshot("03-crash-reporting")

        app.buttons["Yes-CrashReporting"].firstMatch.tap()


        snapshot("04-enable-notifications")
        app.buttons["No-Notifications"].firstMatch.tap()

        snapshot("05-default-settings")
        app.buttons.firstMatch.tap()

        snapshot("1")
    }


    @MainActor
    func testRun() throws {
        let app = XCUIApplication()
        app.launchArguments.append("--skipOnboarding")
        setupSnapshot(app)
        app.launch()

        sleep(2)

        app.buttons.firstMatch.tap()
        snapshot("07-run-tests")

        app.buttons["Run-Button"].tap()
        snapshot("07-run-tests-running")

    }

    @MainActor
    func testSettings() throws {
        // UI tests must launch the application that they test.
        let app = XCUIApplication()
        app.launchArguments.append("--skipOnboarding")
        setupSnapshot(app)
        app.launch()

        sleep(2)

        snapshot("00-dashboard")

        app.buttons["settings"].tap()
        snapshot("09-settings")

        sleep(1)

        app.buttons["notifications"].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        snapshot("10-notifications")

        app.buttons.firstMatch.tap()

        app.buttons["test_options"].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        snapshot("11-test-options")

        app.buttons.firstMatch.tap()

        app.buttons["privacy"].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        snapshot("13-privacy")

        app.buttons.firstMatch.tap()

        app.buttons["proxy"].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        snapshot("14-proxy")

        app.buttons.firstMatch.tap()

        app.buttons["advanced"].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        snapshot("15-advanced")

        app.buttons.firstMatch.tap()

        app.buttons["about_ooni"].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        snapshot("16-about")

        app.buttons.firstMatch.tap()

    }

}
