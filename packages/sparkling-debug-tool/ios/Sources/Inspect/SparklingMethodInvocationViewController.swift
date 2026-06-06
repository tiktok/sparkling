// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit

/// Full-screen panel listing every observed Sparkling Method invocation.
public final class SparklingMethodInvocationViewController: UIViewController {

    /// When `true`, this VC is hosted inside [SparklingInspectorViewController]
    /// and should suppress its own nav-bar items.
    @objc public var embedsWithoutCloseButton: Bool = false

    private let tableView = UITableView(frame: .zero, style: .plain)
    private let searchField = UITextField()
    private let clearButton = UIButton(type: .system)
    private let clearSearchButton = UIButton(type: .system)
    private let toolbar = UIView()
    private let emptyLabel = UILabel()
    private var allInvocations: [SparklingMethodInvocation] = []
    private var filtered: [SparklingMethodInvocation] = []
    private var expanded = Set<String>()
    private var filterText: String = ""

    private lazy var timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss.SSS"
        return f
    }()

    public override func viewDidLoad() {
        super.viewDidLoad()
        title = "Sparkling Method"
        view.backgroundColor = DebugColor.background

        if !embedsWithoutCloseButton {
            navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Close", style: .plain, target: self, action: #selector(dismissTapped))
            navigationItem.rightBarButtonItems = [
                UIBarButtonItem(barButtonSystemItem: .trash, target: self, action: #selector(clearTapped)),
                UIBarButtonItem(barButtonSystemItem: .refresh, target: self, action: #selector(refreshTapped)),
            ]
        }

        configurePanelToolbar()

        tableView.dataSource = self
        tableView.delegate = self
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.estimatedRowHeight = 60
        tableView.rowHeight = UITableView.automaticDimension
        tableView.backgroundColor = DebugColor.background
        tableView.separatorColor = DebugColor.separator
        tableView.alwaysBounceVertical = true
        tableView.register(MethodCell.self, forCellReuseIdentifier: "method")
        view.addSubview(tableView)

        emptyLabel.text = "No Sparkling Method invocations captured yet."
        emptyLabel.textAlignment = .center
        emptyLabel.numberOfLines = 0
        emptyLabel.textColor = DebugColor.secondary
        emptyLabel.font = .systemFont(ofSize: 14)
        emptyLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(emptyLabel)

        let topAnchor = embedsWithoutCloseButton ? view.topAnchor : view.safeAreaLayoutGuide.topAnchor
        NSLayoutConstraint.activate([
            toolbar.topAnchor.constraint(equalTo: topAnchor, constant: 8),
            toolbar.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 12),
            toolbar.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -12),
            toolbar.heightAnchor.constraint(equalToConstant: 40),

            tableView.topAnchor.constraint(equalTo: toolbar.bottomAnchor, constant: 8),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            emptyLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            emptyLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            emptyLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            emptyLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
        ])

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(storeChanged),
            name: SparklingMethodInvocationStore.didChangeNotification,
            object: nil
        )
        refresh()
    }

    @objc private func dismissTapped() { dismiss(animated: true) }
    @objc private func refreshTapped() { refresh() }
    @objc private func clearTapped() { SparklingMethodInvocationStore.shared.clear() }
    @objc private func searchChanged() {
        filterText = searchField.text ?? ""
        applyFilter()
    }
    @objc private func clearSearchTapped() {
        searchField.text = ""
        searchChanged()
    }

    private func configurePanelToolbar() {
        let primary = DebugColor.primary
        let secondary = DebugColor.secondary

        toolbar.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(toolbar)

        searchField.placeholder = "input to filter Sparkling Method"
        searchField.font = .systemFont(ofSize: 14)
        searchField.textColor = primary
        searchField.tintColor = primary
        searchField.backgroundColor = DebugColor.fill
        searchField.layer.cornerRadius = 8
        searchField.layer.masksToBounds = true
        searchField.clearButtonMode = .never
        searchField.returnKeyType = .done
        searchField.delegate = self
        searchField.addTarget(self, action: #selector(searchChanged), for: .editingChanged)
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
        clearButton.addTarget(self, action: #selector(clearTapped), for: .touchUpInside)
        clearButton.translatesAutoresizingMaskIntoConstraints = false

        toolbar.addSubview(searchField)
        toolbar.addSubview(clearButton)

        NSLayoutConstraint.activate([
            clearButton.leadingAnchor.constraint(equalTo: toolbar.leadingAnchor),
            clearButton.topAnchor.constraint(equalTo: toolbar.topAnchor),
            clearButton.bottomAnchor.constraint(equalTo: toolbar.bottomAnchor),
            clearButton.widthAnchor.constraint(equalToConstant: 40),
            searchField.leadingAnchor.constraint(equalTo: clearButton.trailingAnchor, constant: 12),
            searchField.trailingAnchor.constraint(equalTo: toolbar.trailingAnchor),
            searchField.topAnchor.constraint(equalTo: toolbar.topAnchor),
            searchField.bottomAnchor.constraint(equalTo: toolbar.bottomAnchor),
        ])
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
        clearSearchButton.addTarget(self, action: #selector(clearSearchTapped), for: .touchUpInside)
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

    @objc private func storeChanged() { refresh() }

    private func refresh() {
        allInvocations = SparklingMethodInvocationStore.shared.snapshot().reversed()
        applyFilter()
    }

    private func applyFilter() {
        let needle = filterText.trimmingCharacters(in: .whitespaces).lowercased()
        if needle.isEmpty {
            filtered = allInvocations
        } else {
            filtered = allInvocations.filter {
                $0.name.lowercased().contains(needle)
                    || ($0.namespace?.lowercased().contains(needle) ?? false)
            }
        }
        tableView.reloadData()
        emptyLabel.isHidden = !filtered.isEmpty
    }
}

extension SparklingMethodInvocationViewController: UITableViewDataSource, UITableViewDelegate {
    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { filtered.count }

    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "method", for: indexPath) as! MethodCell
        let item = filtered[indexPath.row]
        cell.configure(with: item, expanded: expanded.contains(item.id), timeFormatter: timeFormatter)
        return cell
    }

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        let id = filtered[indexPath.row].id
        if expanded.contains(id) { expanded.remove(id) } else { expanded.insert(id) }
        tableView.reloadRows(at: [indexPath], with: .none)
    }

    @available(iOS 13.0, *)
    public func tableView(_ tableView: UITableView, contextMenuConfigurationForRowAt indexPath: IndexPath, point: CGPoint) -> UIContextMenuConfiguration? {
        let item = filtered[indexPath.row]
        return UIContextMenuConfiguration(identifier: nil, previewProvider: nil) { _ in
            UIMenu(title: "", children: [
                UIAction(title: "Copy") { _ in
                    UIPasteboard.general.string = "\(item.name)\n» params\n\(item.params)\n« result\n\(item.result ?? "")"
                },
            ])
        }
    }
}

extension SparklingMethodInvocationViewController: UITextFieldDelegate {
    public func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
}

private final class MethodCell: UITableViewCell {
    private let nameLabel = UILabel()
    private let durationLabel = UILabel()
    private let metaLabel = UILabel()
    private let payloadLabel = UILabel()
    private let statusBadge = PaddingLabel()
    private let copyButton = UIButton(type: .system)
    private var copyText = ""

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = DebugColor.background
        contentView.backgroundColor = DebugColor.background
        selectionStyle = .none

        nameLabel.font = UIFont(name: "Menlo-Bold", size: 13) ?? .boldSystemFont(ofSize: 13)
        nameLabel.textColor = DebugColor.primary
        nameLabel.numberOfLines = 1

        durationLabel.font = .systemFont(ofSize: 11)
        durationLabel.textColor = DebugColor.secondary
        durationLabel.setContentCompressionResistancePriority(.required, for: .horizontal)

        metaLabel.font = .systemFont(ofSize: 10)
        metaLabel.textColor = DebugColor.secondary
        metaLabel.numberOfLines = 1

        payloadLabel.font = UIFont(name: "Menlo", size: 11) ?? .systemFont(ofSize: 11)
        payloadLabel.textColor = DebugColor.primary
        payloadLabel.numberOfLines = 0

        statusBadge.font = .boldSystemFont(ofSize: 10)
        statusBadge.textColor = .white
        statusBadge.layer.cornerRadius = 4
        statusBadge.layer.masksToBounds = true
        statusBadge.padding = UIEdgeInsets(top: 2, left: 6, bottom: 2, right: 6)
        statusBadge.setContentHuggingPriority(.required, for: .horizontal)

        if #available(iOS 13.0, *) {
            copyButton.setImage(UIImage(systemName: "doc.on.doc")?.withConfiguration(UIImage.SymbolConfiguration(pointSize: 16, weight: .regular)), for: .normal)
        } else {
            copyButton.setTitle("⧉", for: .normal)
        }
        copyButton.tintColor = DebugColor.primary
        copyButton.widthAnchor.constraint(equalToConstant: 32).isActive = true
        copyButton.heightAnchor.constraint(equalToConstant: 32).isActive = true
        copyButton.addTarget(self, action: #selector(copyTapped), for: .touchUpInside)

        let topRow = UIStackView(arrangedSubviews: [statusBadge, nameLabel, durationLabel, copyButton])
        topRow.axis = .horizontal
        topRow.alignment = .center
        topRow.spacing = 8

        let stack = UIStackView(arrangedSubviews: [topRow, metaLabel, payloadLabel])
        stack.axis = .vertical
        stack.spacing = 4
        stack.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
            stack.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -8),
            stack.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }

    func configure(with record: SparklingMethodInvocation, expanded: Bool, timeFormatter: DateFormatter) {
        nameLabel.text = record.name
        durationLabel.text = record.endTime == nil ? "running" : "\(record.durationMs ?? 0)ms"
        var meta = "\(timeFormatter.string(from: record.startTime)) · \(record.platform)"
        if let ns = record.namespace, !ns.isEmpty { meta += " · \(ns)" }
        if let code = record.statusCode { meta += " · code=\(code)" }
        if let msg = record.statusMessage, !msg.isEmpty { meta += " · \(msg)" }
        metaLabel.text = meta

        if expanded {
            payloadLabel.isHidden = false
            payloadLabel.text = "» params\n\(record.params.isEmpty ? "(empty)" : record.params)\n\n« result\n\(record.result?.isEmpty == false ? record.result! : (record.endTime == nil ? "(pending)" : "(empty)"))"
        } else {
            payloadLabel.isHidden = true
        }

        switch record.success {
        case .none:
            statusBadge.text = "·"
            statusBadge.backgroundColor = UIColor(red: 1.0, green: 0.78, blue: 0.34, alpha: 1)
        case .some(false):
            statusBadge.text = "ERR"
            statusBadge.backgroundColor = UIColor(red: 0.9, green: 0.28, blue: 0.30, alpha: 1)
        case .some(true):
            statusBadge.text = "OK"
            statusBadge.backgroundColor = UIColor(red: 0.19, green: 0.65, blue: 0.42, alpha: 1)
        }
        copyText = "\(record.name)\n» params\n\(record.params)\n« result\n\(record.result ?? "")"
    }

    @objc private func copyTapped() {
        UIPasteboard.general.string = copyText
    }
}

private final class PaddingLabel: UILabel {
    var padding: UIEdgeInsets = .zero

    override func drawText(in rect: CGRect) {
        super.drawText(in: rect.inset(by: padding))
    }

    override var intrinsicContentSize: CGSize {
        let size = super.intrinsicContentSize
        return CGSize(
            width: size.width + padding.left + padding.right,
            height: size.height + padding.top + padding.bottom
        )
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
}
