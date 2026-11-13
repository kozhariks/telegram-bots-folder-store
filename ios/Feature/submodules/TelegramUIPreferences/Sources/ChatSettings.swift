import Foundation
import TelegramCore
import SwiftSignalKit

public struct ChatSettings: Codable, Equatable {
    public let sendWithCmdEnter: Bool
    /// When true, a bots-only chat folder shows a Mini App icon grid instead of the dialog list.
    public let botsFolderStoreMode: Bool
    
    public static var defaultSettings: ChatSettings {
        return ChatSettings(sendWithCmdEnter: false, botsFolderStoreMode: true)
    }
    
    public init(sendWithCmdEnter: Bool, botsFolderStoreMode: Bool = true) {
        self.sendWithCmdEnter = sendWithCmdEnter
        self.botsFolderStoreMode = botsFolderStoreMode
    }
    
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: StringCodingKey.self)

        self.sendWithCmdEnter = (try container.decode(Int32.self, forKey: "sendWithCmdEnter")) != 0
        if let value = try container.decodeIfPresent(Int32.self, forKey: "botsFolderStoreMode") {
            self.botsFolderStoreMode = value != 0
        } else {
            self.botsFolderStoreMode = true
        }
    }
    
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringCodingKey.self)

        try container.encode((self.sendWithCmdEnter ? 1 : 0) as Int32, forKey: "sendWithCmdEnter")
        try container.encode((self.botsFolderStoreMode ? 1 : 0) as Int32, forKey: "botsFolderStoreMode")
    }
    
    public static func ==(lhs: ChatSettings, rhs: ChatSettings) -> Bool {
        return lhs.sendWithCmdEnter == rhs.sendWithCmdEnter
            && lhs.botsFolderStoreMode == rhs.botsFolderStoreMode
    }
    
    public func withUpdatedSendWithCmdEnter(_ sendWithCmdEnter: Bool) -> ChatSettings {
        return ChatSettings(sendWithCmdEnter: sendWithCmdEnter, botsFolderStoreMode: self.botsFolderStoreMode)
    }
    
    public func withUpdatedBotsFolderStoreMode(_ botsFolderStoreMode: Bool) -> ChatSettings {
        return ChatSettings(sendWithCmdEnter: self.sendWithCmdEnter, botsFolderStoreMode: botsFolderStoreMode)
    }
}

public func updateChatSettingsInteractively(accountManager: AccountManager<TelegramAccountManagerTypes>, _ f: @escaping (ChatSettings) -> ChatSettings) -> Signal<Void, NoError> {
    return accountManager.transaction { transaction -> Void in
        transaction.updateSharedData(ApplicationSpecificSharedDataKeys.chatSettings, { entry in
            let currentSettings: ChatSettings
            if let entry = entry?.get(ChatSettings.self) {
                currentSettings = entry
            } else {
                currentSettings = ChatSettings.defaultSettings
            }
            return SharedPreferencesEntry(f(currentSettings))
        })
    }
}
