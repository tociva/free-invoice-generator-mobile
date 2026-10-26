import SwiftUI

@main
struct FreeInvoiceGeneratorApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                // The Compose controller owns its own safe-area-aware content. Let
                // its background reach the edges of the UIKit window so iOS does
                // not expose the window's default black background above/below it.
                .ignoresSafeArea(.all)
        }
    }
}
