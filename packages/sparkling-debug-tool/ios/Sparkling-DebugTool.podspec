require 'json'

package = JSON.parse(File.read(File.join(__dir__, '..', 'package.json')))

Pod::Spec.new do |s|
  s.name           = 'Sparkling-DebugTool'
  s.version        = package['version']
  s.summary        = package['description']
  s.description    = package['description']
  s.license        = { :type => 'Apache-2.0' }
  s.author         = 'TikTok'
  s.homepage       = 'https://github.com/tiktok/sparkling'
  s.platforms      = { :ios => '12.0' }
  s.swift_version  = '5.7'
  s.source         = { git: 'https://github.com/tiktok/sparkling.git', tag: s.version.to_s }
  s.static_framework = true

  s.source_files = [
    'Sources/**/*.{h,m,swift}'
  ]

  s.dependency 'Lynx', '~> 3.6.0'
  s.dependency 'LynxService/Devtool', '~> 3.6.0'
  s.dependency 'LynxDevtool/Framework', '~> 3.6.0'
end
