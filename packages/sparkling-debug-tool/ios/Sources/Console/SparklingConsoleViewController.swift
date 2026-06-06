// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import UIKit

/// JS Console panel UI for iOS. Mirrors the Android `SparklingConsoleFragment`:
/// top toolbar (clear / filter / reverse / close) + level segmented control +
/// log table view with single-tap expand and long-press copy.
@objc public final class SparklingConsoleViewController: UIViewController {

    /// When `true`, the Console VC suppresses its own internal close button so
    /// it can be embedded inside [SparklingInspectorViewController]. Defaults
    /// to `false` for backward compatibility.
    @objc public var embedsWithoutCloseButton: Bool = false

    private let levelTabBar = UIStackView()
    private let searchField = UITextField()
    private let clearButton = UIButton(type: .system)
    private let clearSearchButton = UIButton(type: .system)
    private let closeButton = UIButton(type: .system)
    private let tableView = UITableView(frame: .zero, style: .plain)

    private var minLevel: SparklingConsoleLevel = .verbose
    private var keyword: String = ""
    private var reversed: Bool = false
    private var visibleLogs: [SparklingConsoleLog] = []
    private var autoScroll: Bool = true
    private var refreshScheduled: Bool = false
    private var levelButtons: [UIButton] = []
    private let levelItems: [(String, SparklingConsoleLevel)] = [
        ("All", .verbose),
        ("Info", .info),
        ("Debug", .debug),
        ("Warn", .warn),
        ("Error", .error),
    ]

    public override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = DebugColor.background
        title = "JS Console"

        configureToolbar()
        configureTableView()
        applyConstraints()

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(onStoreChanged),
            name: SparklingConsoleStore.didChangeNotification,
            object: nil
        )

        refreshContent(animated: false)
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    // MARK: - Setup

    private func configureToolbar() {
        let primary = DebugColor.primary
        let secondary = DebugColor.secondary

        // Search field — full custom UITextField so we get the same height as
        // the surrounding compact buttons. Magnifier sits on the leftView so
        // the placeholder and value align with the rest of the toolbar.
        searchField.placeholder = "input to filter log"
        searchField.font = .systemFont(ofSize: 14)
        searchField.textColor = primary
        searchField.tintColor = primary
        searchField.backgroundColor = DebugColor.fill
        searchField.layer.cornerRadius = 8
        searchField.layer.masksToBounds = true
        searchField.clearButtonMode = .never
        searchField.returnKeyType = .done
        searchField.delegate = self
        searchField.addTarget(self, action: #selector(onSearchChanged), for: .editingChanged)
        searchField.translatesAutoresizingMaskIntoConstraints = false
        searchField.leftView = makeSearchIconView(color: secondary)
        searchField.leftViewMode = .always
        searchField.rightView = makeClearSearchButton()
        searchField.rightViewMode = .whileEditing
        clearButton.setTitle(nil, for: .normal)
        if #available(iOS 13.0, *) {
            clearButton.setImage(UIImage(systemName: "trash")?.withConfiguration(UIImage.SymbolConfiguration(pointSize: 16, weight: .regular)), for: .normal)
        } else {
            clearButton.setTitle("⌫", for: .normal)
        }
        clearButton.tintColor = primary
        clearButton.backgroundColor = .clear
        clearButton.addTarget(self, action: #selector(onClear), for: .touchUpInside)
        clearButton.translatesAutoresizingMaskIntoConstraints = false

        closeButton.setTitle("Close", for: .normal)
        closeButton.titleLabel?.font = .systemFont(ofSize: 13, weight: .medium)
        closeButton.setTitleColor(primary, for: .normal)
        closeButton.tintColor = primary
        closeButton.backgroundColor = DebugColor.fill
        closeButton.layer.cornerRadius = 0
        closeButton.layer.masksToBounds = true
        closeButton.contentEdgeInsets = UIEdgeInsets(top: 0, left: 12, bottom: 0, right: 12)
        closeButton.addTarget(self, action: #selector(onClose), for: .touchUpInside)
        closeButton.translatesAutoresizingMaskIntoConstraints = false

        configureLevelTabBar()

        view.addSubview(searchField)
        view.addSubview(clearButton)
        view.addSubview(closeButton)
        view.addSubview(levelTabBar)

        if embedsWithoutCloseButton {
            closeButton.isHidden = true
        }
    }

    private func configureLevelTabBar() {
        levelTabBar.axis = .horizontal
        levelTabBar.distribution = .fillEqually
        levelTabBar.spacing = 0
        levelTabBar.backgroundColor = DebugColor.background
        levelTabBar.translatesAutoresizingMaskIntoConstraints = false

        levelButtons = levelItems.enumerated().map { index, item in
            let button = UIButton(type: .system)
            button.tag = index
            button.setTitle(item.0, for: .normal)
            button.titleLabel?.font = .systemFont(ofSize: 13, weight: .medium)
            button.backgroundColor = DebugColor.background
            button.layer.cornerRadius = 0
            button.layer.masksToBounds = true
            button.addTarget(self, action: #selector(onLevelTabTapped(_:)), for: .touchUpInside)
            levelTabBar.addArrangedSubview(button)
            return button
        }
        applyLevelSelection()
    }

    private func makeClearSearchButton() -> UIView {
        let container = UIView(frame: CGRect(x: 0, y: 0, width: 32, height: 28))
        clearSearchButton.frame = CGRect(x: 2, y: 2, width: 24, height: 24)
        clearSearchButton.tintColor = DebugColor.secondary
        if #available(iOS 13.0, *) {
            clearSearchButton.setImage(
                UIImage(systemName: "xmark")?
                    .withConfiguration(UIImage.SymbolConfiguration(pointSize: 11, weight: .semibold)),
                for: .normal
            )
        } else {
            clearSearchButton.setTitle("×", for: .normal)
            clearSearchButton.setTitleColor(DebugColor.secondary, for: .normal)
        }
        clearSearchButton.addTarget(self, action: #selector(onClearSearch), for: .touchUpInside)
        container.addSubview(clearSearchButton)
        return container
    }

    private func makeSearchIconView(color: UIColor) -> UIView {
        let container = UIView(frame: CGRect(x: 0, y: 0, width: 32, height: 24))
        guard #available(iOS 13.0, *) else { return container }
        let imageView = UIImageView(
            image: UIImage(systemName: "magnifyingglass")?
                .withConfiguration(UIImage.SymbolConfiguration(pointSize: 13, weight: .regular))
        )
        imageView.tintColor = color
        imageView.contentMode = .scaleAspectFit
        imageView.frame = CGRect(x: 10, y: 4, width: 16, height: 16)
        container.addSubview(imageView)
        return container
    }

    private func configureTableView() {
        tableView.backgroundColor = DebugColor.background
        tableView.dataSource = self
        tableView.delegate = self
        tableView.separatorStyle = .singleLine
        tableView.separatorColor = DebugColor.separator
        tableView.estimatedRowHeight = 40
        tableView.rowHeight = UITableView.automaticDimension
        tableView.alwaysBounceVertical = true
        tableView.register(SparklingConsoleCell.self, forCellReuseIdentifier: SparklingConsoleCell.reuseId)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableView)
    }

    private func applyConstraints() {
        // Use the view's own top in the embedded case so the toolbar sits
        // immediately under the inspector tab bar; only honor the device safe
        // area when running standalone (which still happens for legacy hosts).
        let topAnchor: NSLayoutYAxisAnchor = embedsWithoutCloseButton
            ? view.topAnchor
            : view.safeAreaLayoutGuide.topAnchor
        let leadingAnchor: NSLayoutXAxisAnchor = view.safeAreaLayoutGuide.leadingAnchor
        let trailingAnchor: NSLayoutXAxisAnchor = view.safeAreaLayoutGuide.trailingAnchor

        let toolbarHeight: CGFloat = 40
        let levelHeight: CGFloat = 44

        var constraints: [NSLayoutConstraint] = [
            // Row 1: trash icon + divider + filter input.
            clearButton.topAnchor.constraint(equalTo: topAnchor, constant: 8),
            clearButton.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 12),
            clearButton.widthAnchor.constraint(equalToConstant: 40),
            clearButton.heightAnchor.constraint(equalToConstant: toolbarHeight),

            searchField.centerYAnchor.constraint(equalTo: clearButton.centerYAnchor),
            searchField.leadingAnchor.constraint(equalTo: clearButton.trailingAnchor, constant: 8),
            searchField.heightAnchor.constraint(equalToConstant: toolbarHeight),

            // Row 2: All / Info / Debug / Warn / Error.
            levelTabBar.topAnchor.constraint(equalTo: clearButton.bottomAnchor, constant: 8),
            levelTabBar.leadingAnchor.constraint(equalTo: leadingAnchor),
            levelTabBar.trailingAnchor.constraint(equalTo: trailingAnchor),
            levelTabBar.heightAnchor.constraint(equalToConstant: levelHeight),

            tableView.topAnchor.constraint(equalTo: levelTabBar.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ]

        if embedsWithoutCloseButton {
            constraints.append(searchField.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -12))
        } else {
            constraints.append(contentsOf: [
                closeButton.centerYAnchor.constraint(equalTo: searchField.centerYAnchor),
                closeButton.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -12),
                closeButton.heightAnchor.constraint(equalToConstant: 32),
                searchField.trailingAnchor.constraint(equalTo: closeButton.leadingAnchor, constant: -8),
            ])
        }

        NSLayoutConstraint.activate(constraints)
    }

    // MARK: - Actions

    @objc private func onClear() {
        SparklingConsoleStore.shared.clear()
    }

    @objc private func onClose() {
        if let nav = navigationController, nav.viewControllers.first !== self {
            nav.popViewController(animated: true)
        } else {
            dismiss(animated: true)
        }
    }

    @objc private func onLevelTabTapped(_ sender: UIButton) {
        let idx = max(0, min(sender.tag, levelItems.count - 1))
        minLevel = levelItems[idx].1
        applyLevelSelection()
        refreshContent(animated: false)
    }

    private func applyLevelSelection() {
        for (index, button) in levelButtons.enumerated() {
            let selected = levelItems[index].1 == minLevel
            button.setTitleColor(selected ? DebugColor.accent : DebugColor.secondary, for: .normal)
            button.backgroundColor = DebugColor.background
        }
    }

    @objc private func onSearchChanged() {
        keyword = searchField.text ?? ""
        refreshContent(animated: false)
    }

    @objc private func onClearSearch() {
        searchField.text = ""
        onSearchChanged()
    }

    @objc private func onStoreChanged() {
        guard !refreshScheduled else { return }
        refreshScheduled = true
        DispatchQueue.main.async { [weak self] in
            self?.refreshScheduled = false
            self?.refreshContent(animated: false)
        }
    }

    private func refreshContent(animated: Bool) {
        let snapshot = SparklingConsoleStore.shared.snapshot()
        let filtered = snapshot.filter { log in
            log.level.rawValue >= minLevel.rawValue && log.matches(keyword: keyword)
        }
        visibleLogs = reversed ? filtered.reversed() : filtered
        tableView.reloadData()

        guard autoScroll, !visibleLogs.isEmpty else { return }
        let lastIndex = reversed ? 0 : visibleLogs.count - 1
        let indexPath = IndexPath(row: lastIndex, section: 0)
        tableView.scrollToRow(at: indexPath, at: .bottom, animated: animated)
    }
}

extension SparklingConsoleViewController: UITextFieldDelegate {
    public func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
}

extension SparklingConsoleViewController: UITableViewDataSource, UITableViewDelegate {
    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return visibleLogs.count
    }

    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: SparklingConsoleCell.reuseId, for: indexPath) as! SparklingConsoleCell
        let log = visibleLogs[indexPath.row]
        cell.configure(with: log)
        return cell
    }

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        var log = visibleLogs[indexPath.row]
        log.expanded.toggle()
        visibleLogs[indexPath.row] = log
        SparklingConsoleStore.shared.update(log)
        tableView.beginUpdates()
        tableView.reloadRows(at: [indexPath], with: .none)
        tableView.endUpdates()
    }

    public func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        autoScroll = false
    }

    public func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        updateAutoScroll()
    }

    public func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        if !decelerate { updateAutoScroll() }
    }

    private func updateAutoScroll() {
        let offsetY = tableView.contentOffset.y + tableView.bounds.height
        let contentHeight = tableView.contentSize.height
        autoScroll = offsetY >= contentHeight - 24
    }

    @available(iOS 13.0, *)
    public func tableView(_ tableView: UITableView, contextMenuConfigurationForRowAt indexPath: IndexPath, point: CGPoint) -> UIContextMenuConfiguration? {
        let log = visibleLogs[indexPath.row]
        return UIContextMenuConfiguration(identifier: nil, previewProvider: nil) { _ in
            let copy = UIAction(title: "Copy") { _ in
                UIPasteboard.general.string = log.message
            }
            return UIMenu(title: "", children: [copy])
        }
    }
}

private final class SparklingConsoleCell: UITableViewCell {
    static let reuseId = "SparklingConsoleCell"

    private let label = UILabel()
    private let copyButton = UIButton(type: .system)
    private var copiedText: String = ""

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = DebugColor.background
        contentView.backgroundColor = DebugColor.background
        selectionStyle = .none

        label.font = .systemFont(ofSize: 14)
        // Wrap by default and cap at 4 lines collapsed; tap toggles full text.
        label.numberOfLines = 4
        label.lineBreakMode = .byWordWrapping
        label.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(label)

        if #available(iOS 13.0, *) {
            copyButton.setImage(UIImage(systemName: "doc.on.doc")?.withConfiguration(UIImage.SymbolConfiguration(pointSize: 16, weight: .regular)), for: .normal)
        } else {
            copyButton.setTitle("⧉", for: .normal)
        }
        copyButton.tintColor = DebugColor.primary
        copyButton.translatesAutoresizingMaskIntoConstraints = false
        copyButton.addTarget(self, action: #selector(copyTapped), for: .touchUpInside)
        contentView.addSubview(copyButton)

        NSLayoutConstraint.activate([
            label.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
            label.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -8),
            label.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 12),
            label.trailingAnchor.constraint(equalTo: copyButton.leadingAnchor, constant: -8),
            copyButton.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            copyButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -8),
            copyButton.widthAnchor.constraint(equalToConstant: 32),
            copyButton.heightAnchor.constraint(equalToConstant: 32),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }

    func configure(with log: SparklingConsoleLog) {
        label.numberOfLines = log.expanded ? 0 : 4
        label.text = formatLine(for: log)
        label.textColor = color(for: log.level)
        copiedText = label.text ?? log.message
    }

    @objc private func copyTapped() {
        UIPasteboard.general.string = copiedText
    }

    private func formatLine(for log: SparklingConsoleLog) -> String {
        return log.tag.isEmpty ? log.message : "[\(log.tag)]\(log.message)"
    }

    private func color(for level: SparklingConsoleLevel) -> UIColor {
        switch level {
        case .verbose: return DebugColor.secondary
        case .debug: return UIColor(red: 0.11, green: 0.30, blue: 0.85, alpha: 1.0)
        case .info: return DebugColor.primary
        case .warn: return UIColor(red: 0.70, green: 0.33, blue: 0.0, alpha: 1.0)
        case .error: return UIColor(red: 0.73, green: 0.11, blue: 0.11, alpha: 1.0)
        }
    }
}

private enum DebugColor {
    static var background: UIColor {
        if #available(iOS 13.0, *) { return .systemBackground }
        return .white
    }
    static var fill: UIColor {
        if #available(iOS 13.0, *) { return .tertiarySystemFill }
        return UIColor(white: 0, alpha: 0.06)
    }
    static var primary: UIColor {
        if #available(iOS 13.0, *) { return .label }
        return UIColor(white: 0.1, alpha: 1)
    }
    static var secondary: UIColor {
        if #available(iOS 13.0, *) { return .secondaryLabel }
        return UIColor(white: 0.48, alpha: 1)
    }
    static var separator: UIColor {
        if #available(iOS 13.0, *) { return .separator }
        return UIColor(white: 0, alpha: 0.1)
    }
    static var accent: UIColor {
        if #available(iOS 13.0, *) { return .systemBlue }
        return UIColor(red: 0.0, green: 0.43, blue: 1.0, alpha: 1.0)
    }
}
