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
    func testMakeScreenshots() throws {
        // UI tests must launch the application that they test.
        let app = XCUIApplication()
        // app.launchArguments += ["-first_run", "NO"]
        setupSnapshot(app)
        app.launch()

        sleep(5)

        snapshot("00-what-is-ooni-probe")

        app.buttons.firstMatch.tap()

        snapshot("01-things-to-know")

        app.buttons.firstMatch.tap()
        sleep(1)

        // Quiz buttons reports as not clickable
        print("quiz button is clickable : \(app.buttons["Quiz-True"].isHittable)")

        let trueButton = app.buttons.firstMatch
        sleep(1)
        trueButton.tap()

        sleep(1)
        app.buttons.element(boundBy: app.buttons.count).tap()

        snapshot("02-automated-testing")

        app.buttons["No-AutoTest"].firstMatch.tap()

        snapshot("03-crash-reporting")

        app.buttons["Yes-CrashReporting"].firstMatch.tap()


        snapshot("04-enable-notifications")
        app.buttons["No-Notifications"].firstMatch.tap()

        snapshot("05-default-settings")
        app.buttons.firstMatch.tap()

        snapshot("1")

        // Use XCTAssert and related functions to verify your tests produce the correct results.
    }

}
