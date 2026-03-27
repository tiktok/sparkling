Pod::Spec.new do |s|
  source_root      = 'packages/sparkling-sdk/ios'
  source_globs     = ->(patterns) { patterns.flat_map { |pattern| [pattern, "#{source_root}/#{pattern}"] } }
  s.name           = 'Sparkling'
  s.version        = "2.1.0-rc.6"
  s.summary        = "iOS SDK for Sparkling Framework"
  s.description    = "Main iOS framework for Sparkling, including app container, services, and Lynx integration."
  s.license        = "Apache 2.0"
  s.author         = "junchen.ge"
  s.homepage       = 'https://github.com/tiktok/sparkling.git'
  s.readme         = 'packages/sparkling-sdk/README.md'
  s.platforms      = {
    :ios => '12.0'
  }
  s.swift_version  = '5.10'
  s.source         = { git: 'https://github.com/tiktok/sparkling.git', tag: s.version.to_s }
  s.static_framework = true

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'SWIFT_COMPILATION_MODE' => 'wholemodule',
    'OTHER_SWIFT_FLAGS' => '-enable-experimental-feature SymbolLinkageMarkers'
  }
  
  s.subspec 'Application' do |application|
    application.source_files = source_globs.call([
      'Sparkling/Application/**/*.{swift,m,h}',
    ])
    application.dependency 'Sparkling/Service'
    application.dependency 'Sparkling/Utils'
    application.dependency 'SparklingMethod/Core', s.version.to_s
    application.dependency 'SnapKit'
    
    application.resource_bundle = {
      'sparklingPageResource' => source_globs.call([
        'Sparkling/Application/Container/UI/SparklingPageResource.xcassets',
      ])
    }
    
  end
  
  s.subspec 'Service' do |service|
    service.source_files = source_globs.call([
      'Sparkling/Service/{Base,Protocols}/**/*.{swift,m,h}',
    ])
    service.dependency 'Sparkling/Utils'
    service.dependency 'SparklingMethod/Core', s.version.to_s
    
    service.subspec 'LynxService' do |lynx|
      lynx.dependency 'Lynx/Framework', '3.6.0'
      lynx.dependency 'SparklingMethod/Lynx', s.version.to_s
      lynx.source_files = source_globs.call([
        'Sparkling/Service/{Base,Protocols}/**/*.{swift,m,h}',
        'Sparkling/Service/LynxService/**/*.{swift,m,h}',
      ])
    end
    
  end
  
  s.subspec 'Utils' do |utils|
    utils.source_files = source_globs.call([
      'Sparkling/Utils/**/*.{swift,m,h}',
    ])
  end
end
