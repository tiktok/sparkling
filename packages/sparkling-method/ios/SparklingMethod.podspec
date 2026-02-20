require 'json'

Pod::Spec.new do |s|
  source_root      = 'packages/sparkling-method/ios'
  source_globs     = ->(patterns) { patterns.flat_map { |pattern| [pattern, "#{source_root}/#{pattern}"] } }
  s.name           = 'SparklingMethod'
  s.version        = "2.0.1"
  s.summary        = "iOS SDK for Sparkling Method"
  s.description    = "Core iOS method runtime for Sparkling, with Lynx integration, dependency injection support, and debug helpers."
  s.license        = "Apache 2.0"
  s.author         = "zhangyujie"
  s.homepage       = 'https://github.com/tiktok/sparkling.git'
  s.readme         = 'packages/sparkling-method/README.md'
  s.platforms      = {
    :ios => '12.0'
  }
  s.swift_version  = '5.10'
  s.source         = { git: 'https://github.com/tiktok/sparkling.git', tag: s.version.to_s }
  s.static_framework = true

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'SWIFT_COMPILATION_MODE' => 'wholemodule'
  }
    
  s.subspec 'Core' do |core|
    core.source_files = source_globs.call([
      'Sources/Core/Definitions/*.{h,m,swift}',
      'Sources/Core/Pipe/*.{h,m,swift}',
      'Sources/Core/Models/*.{h,m,swift}',
      'Sources/Core/Protocols/*.{h,m,swift}',
      'Sources/Core/Utils/*.{h,m,swift}',
      'Sources/Core/DI/**/*.{h,m,swift}',
    ])
    core.dependency 'Mantle', '~> 2.2.0'
  end
  
  s.subspec 'Lynx' do |lynx|
    lynx.source_files = source_globs.call([
      'Sources/Lynx/Pipe/*.{h,m,swift}',
      'Sources/Lynx/Engine/*.{h,m,swift}',
      'Sources/Lynx/Definitions/*.{h,m,swift}',
      'Sources/Lynx/Module/*.{h,m,swift}',
      'Sources/Lynx/Models/*.{h,m,swift}',
    ])
    
    lynx.dependency 'SparklingMethod/Core'
    lynx.dependency 'Lynx/Framework', '3.6.0'
    lynx.dependency 'PrimJS/quickjs', '>=2.12.0'
    lynx.dependency 'PrimJS/napi', '>=2.12.0'
  end
  
  s.subspec 'DIProvider' do |di|
    di.source_files = source_globs.call([
      'Sources/DIProvider/**/*.{h,m,swift}',
    ])
    
    di.dependency 'SparklingMethod/Core'
  end
  
  s.subspec 'Debug' do |de|
    de.source_files = source_globs.call([
      'Sources/Debug/**/*.{h,m,swift}',
    ])
    
    de.dependency 'SparklingMethod/Core'
  end
end
