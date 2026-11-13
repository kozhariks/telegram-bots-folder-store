import Foundation
import TelegramCore
import TelegramUIPreferences

/// Helpers for List / Store display mode inside a Bots chat folder.
/// Parity with Android `BotsFolderViewHelper` and Desktop `bots_folder_store`.
public enum BotsFolderStoreHelper {
    /// True when the folder is a pure "Bots" type filter (chat types include only bots).
    public static func isBotsOnlyFilter(_ filter: ChatListFilter?) -> Bool {
        guard case let .filter(_, _, _, data) = filter else {
            return false
        }
        return data.categories == .bots
    }
    
    public static func isStoreMode(_ settings: ChatSettings) -> Bool {
        return settings.botsFolderStoreMode
    }
    
    public static func shouldShowStore(filter: ChatListFilter?, settings: ChatSettings, isSearching: Bool, isEditing: Bool) -> Bool {
        return isBotsOnlyFilter(filter)
            && isStoreMode(settings)
            && !isSearching
            && !isEditing
    }
}
