import Foundation
import UIKit
import AsyncDisplayKit
import Display
import TelegramCore
import SwiftSignalKit
import TelegramPresentationData
import AccountContext
import AvatarNode
import LocalizedPeerData

/// Home-screen style grid of Mini Apps for a Bots folder.
/// Tap → bot chat; long-press → Mini App.
final class BotsFolderStoreNode: ASDisplayNode, UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {
    private struct Item: Equatable {
        let peer: EnginePeer
        let unread: Int
        
        static func ==(lhs: Item, rhs: Item) -> Bool {
            return lhs.peer.id == rhs.peer.id && lhs.unread == rhs.unread
        }
    }
    
    private let context: AccountContext
    private var presentationData: PresentationData
    
    var openBotChat: ((EnginePeer) -> Void)?
    var openBotApp: ((EnginePeer) -> Void)?
    
    private let emptyLabel: ImmediateTextNode
    private var collectionView: UICollectionView?
    private var items: [Item] = []
    
    private var disposable: Disposable?
    private var validLayout: (CGSize, UIEdgeInsets)?
    
    private static let columns: CGFloat = 4.0
    private static let horizontalInset: CGFloat = 12.0
    private static let cellSpacing: CGFloat = 8.0
    
    init(context: AccountContext, presentationData: PresentationData) {
        self.context = context
        self.presentationData = presentationData
        
        self.emptyLabel = ImmediateTextNode()
        self.emptyLabel.isUserInteractionEnabled = false
        self.emptyLabel.maximumNumberOfLines = 0
        self.emptyLabel.textAlignment = .center
        
        super.init()
        
        self.setViewBlock({
            return UITracingLayerView()
        })
        self.backgroundColor = .clear
        self.addSubnode(self.emptyLabel)
        self.updateEmptyText()
    }
    
    deinit {
        self.disposable?.dispose()
    }
    
    override func didLoad() {
        super.didLoad()
        
        let layout = UICollectionViewFlowLayout()
        layout.minimumInteritemSpacing = Self.cellSpacing
        layout.minimumLineSpacing = Self.cellSpacing
        layout.scrollDirection = .vertical
        
        let collectionView = UICollectionView(frame: self.bounds, collectionViewLayout: layout)
        collectionView.backgroundColor = .clear
        collectionView.alwaysBounceVertical = true
        collectionView.showsVerticalScrollIndicator = false
        collectionView.contentInsetAdjustmentBehavior = .never
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.register(BotsFolderStoreCell.self, forCellWithReuseIdentifier: BotsFolderStoreCell.reuseId)
        self.view.addSubview(collectionView)
        self.collectionView = collectionView
        
        let longPress = UILongPressGestureRecognizer(target: self, action: #selector(self.handleLongPress(_:)))
        collectionView.addGestureRecognizer(longPress)
        
        self.reload()
    }
    
    func updatePresentationData(_ presentationData: PresentationData) {
        self.presentationData = presentationData
        self.updateEmptyText()
        self.collectionView?.reloadData()
        if let (size, insets) = self.validLayout {
            self.updateLayout(size: size, insets: insets, transition: .immediate)
        }
    }
    
    func updateLayout(size: CGSize, insets: UIEdgeInsets, transition: ContainedViewLayoutTransition) {
        self.validLayout = (size, insets)
        
        let collectionFrame = CGRect(origin: CGPoint(), size: size)
        if let collectionView = self.collectionView {
            transition.updateFrame(view: collectionView, frame: collectionFrame)
            collectionView.contentInset = insets
            collectionView.scrollIndicatorInsets = insets
        }
        
        let emptySize = self.emptyLabel.updateLayout(CGSize(width: max(1.0, size.width - 48.0), height: size.height))
        let emptyFrame = CGRect(
            origin: CGPoint(x: floor((size.width - emptySize.width) / 2.0), y: floor((size.height - emptySize.height) / 2.0)),
            size: emptySize
        )
        transition.updateFrame(node: self.emptyLabel, frame: emptyFrame)
        
        self.emptyLabel.isHidden = !self.items.isEmpty
        self.collectionView?.isHidden = self.items.isEmpty
    }
    
    func reload() {
        self.disposable?.dispose()
        
        let context = self.context
        let recentApps = context.engine.peers.recentApps()
        let chatList = context.account.viewTracker.tailChatListView(groupId: .root, count: 200)
        
        self.disposable = (combineLatest(queue: .mainQueue(), recentApps, chatList)
        |> mapToSignal { recentIds, chatListView -> Signal<[Item], NoError> in
            var orderedIds: [EnginePeer.Id] = []
            var seen = Set<EnginePeer.Id>()
            
            for id in recentIds {
                if !seen.contains(id) {
                    seen.insert(id)
                    orderedIds.append(id)
                }
            }
            
            for entry in chatListView.0.entries.reversed() {
                if case let .MessageEntry(messageEntry) = entry {
                    let peerId = messageEntry.index.messageIndex.id.peerId
                    if peerId.namespace == Namespaces.Peer.CloudUser, !seen.contains(peerId) {
                        seen.insert(peerId)
                        orderedIds.append(peerId)
                    }
                }
            }
            
            if orderedIds.isEmpty {
                return .single([])
            }
            
            return context.engine.data.subscribe(
                EngineDataMap(orderedIds.map { TelegramEngine.EngineData.Item.Peer.Peer(id: $0) }),
                EngineDataMap(orderedIds.map { TelegramEngine.EngineData.Item.Messages.PeerUnreadState(id: $0) })
            )
            |> map { peers, unreadStates -> [Item] in
                var result: [Item] = []
                for id in orderedIds {
                    guard let maybePeer = peers[id], let peer = maybePeer else {
                        continue
                    }
                    guard case let .user(user) = peer, user.botInfo != nil else {
                        continue
                    }
                    let unreadState = unreadStates[id]
                    var unread = unreadState?.count ?? 0
                    if unread <= 0, unreadState?.isMarkedUnread == true {
                        unread = 1
                    }
                    result.append(Item(peer: peer, unread: max(0, unread)))
                }
                return result
            }
        }
        |> deliverOnMainQueue).startStrict(next: { [weak self] items in
            guard let self else {
                return
            }
            self.items = items
            self.emptyLabel.isHidden = !items.isEmpty
            self.collectionView?.isHidden = items.isEmpty
            self.collectionView?.reloadData()
            if let (size, insets) = self.validLayout {
                self.updateLayout(size: size, insets: insets, transition: .immediate)
            }
        })
    }
    
    private func updateEmptyText() {
        self.emptyLabel.attributedText = NSAttributedString(
            string: "No apps yet\nOpen a Mini App once — it will appear here",
            font: Font.regular(15.0),
            textColor: self.presentationData.theme.list.itemSecondaryTextColor,
            paragraphAlignment: .center
        )
    }
    
    @objc private func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
        guard gesture.state == .began, let collectionView = self.collectionView else {
            return
        }
        let point = gesture.location(in: collectionView)
        guard let indexPath = collectionView.indexPathForItem(at: point), indexPath.item < self.items.count else {
            return
        }
        self.openBotApp?(self.items[indexPath.item].peer)
    }
    
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return self.items.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: BotsFolderStoreCell.reuseId, for: indexPath) as! BotsFolderStoreCell
        let item = self.items[indexPath.item]
        cell.configure(
            context: self.context,
            presentationData: self.presentationData,
            peer: item.peer,
            unread: item.unread
        )
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard indexPath.item < self.items.count else {
            return
        }
        self.openBotChat?(self.items[indexPath.item].peer)
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let width = collectionView.bounds.width
        let available = max(1.0, width - Self.horizontalInset * 2.0 - Self.cellSpacing * (Self.columns - 1.0))
        let side = floor(available / Self.columns)
        return CGSize(width: side, height: side + 28.0)
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, insetForSectionAt section: Int) -> UIEdgeInsets {
        return UIEdgeInsets(top: 8.0, left: Self.horizontalInset, bottom: 8.0, right: Self.horizontalInset)
    }
}

private final class BotsFolderStoreCell: UICollectionViewCell {
    static let reuseId = "BotsFolderStoreCell"
    
    private let avatarNode: AvatarNode
    private let titleNode: ImmediateTextNode
    private let badgeBackground: ASImageNode
    private let badgeText: ImmediateTextNode
    
    override init(frame: CGRect) {
        self.avatarNode = AvatarNode(font: avatarPlaceholderFont(size: 28.0))
        self.titleNode = ImmediateTextNode()
        self.titleNode.maximumNumberOfLines = 2
        self.titleNode.textAlignment = .center
        self.titleNode.isUserInteractionEnabled = false
        
        self.badgeBackground = ASImageNode()
        self.badgeBackground.displaysAsynchronously = false
        self.badgeBackground.displayWithoutProcessing = true
        self.badgeBackground.isUserInteractionEnabled = false
        
        self.badgeText = ImmediateTextNode()
        self.badgeText.isUserInteractionEnabled = false
        self.badgeText.textAlignment = .center
        
        super.init(frame: frame)
        
        self.contentView.addSubview(self.avatarNode.view)
        self.contentView.addSubview(self.titleNode.view)
        self.contentView.addSubview(self.badgeBackground.view)
        self.contentView.addSubview(self.badgeText.view)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func configure(context: AccountContext, presentationData: PresentationData, peer: EnginePeer, unread: Int) {
        let iconSide = min(self.bounds.width, max(48.0, self.bounds.width - 8.0))
        let iconSize = CGSize(width: iconSide, height: iconSide)
        
        self.avatarNode.setPeer(
            context: context,
            theme: presentationData.theme,
            peer: peer,
            emptyColor: presentationData.theme.list.mediaPlaceholderColor,
            clipStyle: .roundedRect,
            synchronousLoad: false,
            displayDimensions: iconSize
        )
        self.avatarNode.frame = CGRect(origin: CGPoint(x: floor((self.bounds.width - iconSide) / 2.0), y: 0.0), size: iconSize)
        
        let title = peer.compactDisplayTitle
        self.titleNode.attributedText = NSAttributedString(
            string: title,
            font: Font.regular(11.0),
            textColor: presentationData.theme.list.itemPrimaryTextColor,
            paragraphAlignment: .center
        )
        let titleSize = self.titleNode.updateLayout(CGSize(width: self.bounds.width, height: 28.0))
        self.titleNode.frame = CGRect(
            origin: CGPoint(x: floor((self.bounds.width - titleSize.width) / 2.0), y: iconSide + 4.0),
            size: titleSize
        )
        
        if unread > 0 {
            let badgeString: String
            if unread > 99 {
                badgeString = "99+"
            } else {
                badgeString = "\(unread)"
            }
            self.badgeText.attributedText = NSAttributedString(
                string: badgeString,
                font: Font.semibold(11.0),
                textColor: presentationData.theme.list.itemCheckColors.foregroundColor
            )
            let textSize = self.badgeText.updateLayout(CGSize(width: 40.0, height: 20.0))
            let badgeWidth = max(18.0, textSize.width + 10.0)
            let badgeHeight: CGFloat = 18.0
            let badgeFrame = CGRect(
                origin: CGPoint(x: self.avatarNode.frame.maxX - badgeWidth + 4.0, y: self.avatarNode.frame.minY - 2.0),
                size: CGSize(width: badgeWidth, height: badgeHeight)
            )
            self.badgeBackground.image = generateStretchableFilledCircleImage(
                diameter: badgeHeight,
                color: presentationData.theme.list.itemCheckColors.fillColor
            )
            self.badgeBackground.frame = badgeFrame
            self.badgeText.frame = CGRect(
                origin: CGPoint(
                    x: badgeFrame.minX + floor((badgeWidth - textSize.width) / 2.0),
                    y: badgeFrame.minY + floor((badgeHeight - textSize.height) / 2.0)
                ),
                size: textSize
            )
            self.badgeBackground.isHidden = false
            self.badgeText.isHidden = false
        } else {
            self.badgeBackground.isHidden = true
            self.badgeText.isHidden = true
        }
    }
}
