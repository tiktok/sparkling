//
//  SPKLoadErrorViewProtocol.swift
//  Sparkling
//
//  Created by gejunchen.ChenJr on 2025/8/28.
//

import Foundation

public typealias SPKLoadErrorRefreshBlock = () -> Void

@objc
public protocol SPKLoadErrorViewProtocol {
    @objc optional func register(refreshBlock: SPKLoadErrorRefreshBlock)
    
    @objc optional func container(_ contianer: SPKContainerProtocol, didReceiveError error: Error?)
    
}
