import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    @Environment(\.scenePhase) private var scenePhase

    init() {
        #if DEBUG
        MainViewControllerKt.setLoggingEnabled(enabled: true)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onChange(of: scenePhase) { _, newPhase in
                    switch newPhase {
                    case .active:
                        MainViewControllerKt.notifyAppForegrounded()
                    case .background:
                        MainViewControllerKt.notifyAppBackgrounded()
                    case .inactive:
                        break
                    @unknown default:
                        break
                    }
                }
        }
    }
}
