import UIKit
import Social
import CoreServices

/// Share controller handles action of receiving share action and
/// launching the app with appropriate arguments ``incomingURL``.
class ShareViewController: UIViewController {

    private let typeText = String(kUTTypeText)
    private let typeURL = String(kUTTypeURL)
    private let appURL = "ooni://nettest"

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        guard let extensionItem = extensionContext?.inputItems.first as? NSExtensionItem,
              let itemProvider = extensionItem.attachments?.first else {
            self.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
            return
        }

        if itemProvider.hasItemConformingToTypeIdentifier(typeText) {
            handleIncomingText(itemProvider: itemProvider)
        } else if itemProvider.hasItemConformingToTypeIdentifier(typeURL) {
            handleIncomingURL(itemProvider: itemProvider)
        } else {
            NSLog("Error: No url or text found")
            self.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
        }
    }

    private func handleIncomingText(itemProvider: NSItemProvider) {
        itemProvider.loadItem(forTypeIdentifier: typeText, options: nil) { (item, error) in
            if let error = error { NSLog("Text-Error: \(error.localizedDescription)") }

            if let text = item as? String {
                do {
                    let detector = try NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue)
                    let matches = detector.matches(
                            in: text,
                            options: [],
                            range: NSRange(location: 0, length: text.utf16.count)
                    )
                    if let firstMatch = matches.first, let range = Range(firstMatch.range, in: text) {
                        self.openMainApp(String(text[range]))
                    }
                } catch let error {
                    NSLog("Do-Try Error: \(error.localizedDescription)")
                }
            }
        }
    }

    private func handleIncomingURL(itemProvider: NSItemProvider) {
        itemProvider.loadItem(forTypeIdentifier: typeURL, options: nil) { (item, error) in
            if let error = error { NSLog("URL-Error: \(error.localizedDescription)") }

            if let url = item as? NSURL, let urlString = url.absoluteString {
                self.openMainApp(urlString)
            }
        }
    }


    private func openMainApp(_ urlString: String) {

        let dict: NSDictionary = [
            "urls": [
                urlString
            ],
        ]
        do {
            let data = try JSONSerialization.data(withJSONObject: dict, options: JSONSerialization.WritingOptions.prettyPrinted)

            let json = NSString(data: data, encoding: String.Encoding.utf8.rawValue)
            let queryItems = [
                URLQueryItem(name: "url", value: urlString)
            ]
            var urlComps = URLComponents(string: appURL)!
            urlComps.queryItems = queryItems
            let result = urlComps.url!
            self.extensionContext?.completeRequest(returningItems: nil, completionHandler: { _ in
                self.openURL(result)
            })
        } catch let error  {
            NSLog("something went wrong with parsing json",error.localizedDescription)
        }
    }

    // Courtesy: https://stackoverflow.com/a/44499222/13363449 👇🏾
    // Function must be named exactly like this so a selector can be found by the compiler!
    // Anyway - it's another selector in another instance that would be "performed" instead.
    @objc private func openURL(_ url: URL) -> Bool {
        var responder: UIResponder? = self
        while responder != nil {
            if let application = responder as? UIApplication {
                return application.perform(#selector(openURL(_:)), with: url) != nil
            }
            responder = responder?.next
        }
        return false
    }
}
