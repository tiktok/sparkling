// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit

/// Full-screen panel that displays GlobalProps + queryItems for every live
/// Lynx instance currently registered with the host's provider.
public final class SparklingGlobalPropsViewController: UIViewController {

    /// When `true`, this VC is hosted inside [SparklingInspectorViewController]
    /// and should suppress its own nav-bar items.
    @objc public var embedsWithoutCloseButton: Bool = false

    private enum Row {
        case section(containerId: String, templateUrl: String?)
        case header(String)
        case kv(key: String, value: String)
        case queryItems(containerKey: String, count: Int, expanded: Bool)
    }

    private let tableView = UITableView(frame: .zero, style: .plain)
    private let searchField = UITextField()
    private let clearSearchButton = UIButton(type: .system)
    private let refreshButton = UIButton(type: .system)
    private let toolbar = UIView()
    private let emptyLabel = UILabel()
    private var rows: [Row] = []
    private var snapshots: [SparklingGlobalPropsSnapshot] = []
    private var expandedQueryItems = Set<String>()
    private var filterText: String = ""

    public override func viewDidLoad() {
        super.viewDidLoad()
        title = "GlobalProps"
        view.backgroundColor = DebugColor.background

        if !embedsWithoutCloseButton {
            navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Close", style: .plain, target: self, action: #selector(dismissTapped))
            navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: .refresh, target: self, action: #selector(refreshTapped))
        }

        configurePanelToolbar()

        tableView.dataSource = self
        tableView.delegate = self
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.estimatedRowHeight = 60
        tableView.rowHeight = UITableView.automaticDimension
        tableView.alwaysBounceVertical = true
        tableView.backgroundColor = DebugColor.background
        tableView.register(KVCell.self, forCellReuseIdentifier: "kv")
        tableView.register(HeaderCell.self, forCellReuseIdentifier: "header")
        tableView.register(SectionCell.self, forCellReuseIdentifier: "section")
        view.addSubview(tableView)

        emptyLabel.text = "No live LynxView found yet. Configure SparklingDebugTool.setGlobalPropsProvider(...) in your host app."
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
            selector: #selector(globalPropsChanged),
            name: SparklingGlobalPropsRegistry.didChangeNotification,
            object: nil
        )
        refresh()
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        refresh()
    }

    @objc private func dismissTapped() { dismiss(animated: true) }

    @objc private func refreshTapped() { refresh() }

    @objc private func searchChanged() {
        filterText = searchField.text ?? ""
        applyFilter()
    }

    @objc private func clearSearchTapped() {
        searchField.text = ""
        searchChanged()
    }

    private func configurePanelToolbar() {
        toolbar.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(toolbar)

        searchField.placeholder = "Filter by key or value"
        searchField.font = .systemFont(ofSize: 14)
        searchField.textColor = DebugColor.primary
        searchField.tintColor = DebugColor.primary
        searchField.backgroundColor = DebugColor.fill
        searchField.layer.cornerRadius = 8
        searchField.layer.masksToBounds = true
        searchField.clearButtonMode = .never
        searchField.returnKeyType = .done
        searchField.delegate = self
        searchField.addTarget(self, action: #selector(searchChanged), for: .editingChanged)
        searchField.translatesAutoresizingMaskIntoConstraints = false
        searchField.leftView = makeSearchIconView(color: DebugColor.secondary)
        searchField.leftViewMode = .always
        searchField.rightView = makeClearSearchButton()
        searchField.rightViewMode = .whileEditing

        toolbar.addSubview(searchField)
        refreshButton.setTitle("Refresh", for: .normal)
        refreshButton.titleLabel?.font = .systemFont(ofSize: 13, weight: .medium)
        refreshButton.setTitleColor(DebugColor.primary, for: .normal)
        refreshButton.backgroundColor = DebugColor.fill
        refreshButton.layer.cornerRadius = 8
        refreshButton.layer.masksToBounds = true
        refreshButton.contentEdgeInsets = UIEdgeInsets(top: 0, left: 12, bottom: 0, right: 12)
        refreshButton.addTarget(self, action: #selector(refreshTapped), for: .touchUpInside)
        refreshButton.translatesAutoresizingMaskIntoConstraints = false
        toolbar.addSubview(refreshButton)
        NSLayoutConstraint.activate([
            searchField.leadingAnchor.constraint(equalTo: toolbar.leadingAnchor),
            searchField.trailingAnchor.constraint(equalTo: refreshButton.leadingAnchor, constant: -8),
            searchField.topAnchor.constraint(equalTo: toolbar.topAnchor),
            searchField.bottomAnchor.constraint(equalTo: toolbar.bottomAnchor),
            refreshButton.trailingAnchor.constraint(equalTo: toolbar.trailingAnchor),
            refreshButton.topAnchor.constraint(equalTo: toolbar.topAnchor),
            refreshButton.bottomAnchor.constraint(equalTo: toolbar.bottomAnchor),
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

    @objc private func globalPropsChanged() {
        refresh()
    }

    func refresh() {
        SparklingDebugAutoWiring.refreshLiveLynxViews()
        snapshots = Array(SparklingGlobalPropsRegistry.shared.collectAll().suffix(1))
        applyFilter()
    }

    private func applyFilter() {
        rows.removeAll()
        let needle = filterText.trimmingCharacters(in: .whitespaces).lowercased()
        for snapshot in snapshots {
            let filteredGlobal = snapshot.globalProps.filter { matches(key: $0.key, value: $0.value, needle: needle) }
            let filteredQuery = snapshot.queryItems.filter { matches(key: $0.key, value: $0.value, needle: needle) }
            if !needle.isEmpty && filteredGlobal.isEmpty && filteredQuery.isEmpty {
                continue
            }
            let containerKey = snapshot.containerId.isEmpty ? (snapshot.templateUrl ?? "") : snapshot.containerId
            rows.append(.section(containerId: snapshot.containerId, templateUrl: snapshot.templateUrl))
            if !filteredGlobal.isEmpty || !filteredQuery.isEmpty {
                rows.append(.header("globalProps"))
                rows.append(contentsOf: filteredGlobal.sorted { $0.key < $1.key }.map { .kv(key: $0.key, value: SparklingMethodInvocation.format($0.value)) })
                if !filteredQuery.isEmpty {
                    let expanded = expandedQueryItems.contains(containerKey) || !needle.isEmpty
                    rows.append(.queryItems(containerKey: containerKey, count: snapshot.queryItems.count, expanded: expanded))
                    if expanded {
                        rows.append(contentsOf: filteredQuery.sorted { $0.key < $1.key }.map { .kv(key: "  \($0.key)", value: SparklingMethodInvocation.format($0.value)) })
                    }
                }
            }
        }
        tableView.reloadData()
        emptyLabel.isHidden = !rows.isEmpty
    }

    private func matches(key: String, value: Any?, needle: String) -> Bool {
        if needle.isEmpty { return true }
        if key.lowercased().contains(needle) { return true }
        return ((value.map { "\($0)" }) ?? "").lowercased().contains(needle)
    }
}

extension SparklingGlobalPropsViewController: UITableViewDataSource, UITableViewDelegate {
    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { rows.count }

    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        switch rows[indexPath.row] {
        case .section(let id, let url):
            let cell = tableView.dequeueReusableCell(withIdentifier: "section", for: indexPath) as! SectionCell
            cell.titleLabel.text = "container: \(id)"
            cell.subtitleLabel.text = url ?? ""
            cell.subtitleLabel.isHidden = (url ?? "").isEmpty
            return cell
        case .header(let title):
            let cell = tableView.dequeueReusableCell(withIdentifier: "header", for: indexPath) as! HeaderCell
            cell.titleLabel.text = title
            return cell
        case .kv(let key, let value):
            let cell = tableView.dequeueReusableCell(withIdentifier: "kv", for: indexPath) as! KVCell
            cell.keyLabel.text = key
            cell.valueLabel.text = value
            return cell
        case .queryItems(_, let count, let expanded):
            let cell = tableView.dequeueReusableCell(withIdentifier: "kv", for: indexPath) as! KVCell
            cell.keyLabel.text = expanded ? "queryItems ▾" : "queryItems ▸"
            cell.valueLabel.text = "Array(\(count))"
            return cell
        }
    }

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        switch rows[indexPath.row] {
        case .queryItems(let key, _, _):
            if expandedQueryItems.contains(key) {
                expandedQueryItems.remove(key)
            } else {
                expandedQueryItems.insert(key)
            }
            applyFilter()
        case .kv(let key, let value):
            UIPasteboard.general.string = "\(key)=\(value)"
        default:
            break
        }
    }
}

extension SparklingGlobalPropsViewController: UITextFieldDelegate {
    public func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
}

private final class SectionCell: UITableViewCell {
    let titleLabel = UILabel()
    let subtitleLabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = DebugColor.secondaryBackground
        titleLabel.font = .boldSystemFont(ofSize: 14)
        titleLabel.textColor = DebugColor.primary
        subtitleLabel.font = .systemFont(ofSize: 12)
        subtitleLabel.textColor = DebugColor.secondary
        let stack = UIStackView(arrangedSubviews: [titleLabel, subtitleLabel])
        stack.axis = .vertical
        stack.spacing = 2
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
}

private final class HeaderCell: UITableViewCell {
    let titleLabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = DebugColor.secondaryBackground
        titleLabel.font = .boldSystemFont(ofSize: 13)
        titleLabel.textColor = DebugColor.primary
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(titleLabel)
        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 6),
            titleLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6),
            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            titleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
        ])
    }
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }
}

private final class KVCell: UITableViewCell {
    let keyLabel = UILabel()
    let valueLabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = DebugColor.background
        keyLabel.font = UIFont(name: "Menlo-Bold", size: 12) ?? .boldSystemFont(ofSize: 12)
        keyLabel.textColor = DebugColor.primary
        keyLabel.numberOfLines = 1
        valueLabel.font = UIFont(name: "Menlo", size: 12) ?? .systemFont(ofSize: 12)
        valueLabel.textColor = DebugColor.secondary
        valueLabel.numberOfLines = 0
        let stack = UIStackView(arrangedSubviews: [keyLabel, valueLabel])
        stack.axis = .vertical
        stack.spacing = 2
        stack.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 6),
            stack.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6),
            stack.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
        ])
    }
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }
}

private enum DebugColor {
    static var background: UIColor {
        if #available(iOS 13.0, *) { return .systemBackground }
        return .white
    }
    static var secondaryBackground: UIColor {
        if #available(iOS 13.0, *) { return .secondarySystemBackground }
        return UIColor(white: 0.95, alpha: 1)
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
}
