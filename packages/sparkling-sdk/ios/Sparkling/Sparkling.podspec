Pod::Spec.new do |s|
  s.name           = 'Sparkling'
  s.version        = "2.1.0-rc.12"
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

  # The macro plugin binary is resolved relative to the Sparkling pod root.
  # This assumes Sparkling and SparklingMacro are installed as sibling pods under
  # the same CocoaPods sandbox layout.
  # If your project uses a monorepo, custom Podfile wiring, or any non-standard
  # directory structure, update this relative path to match where
  # SparklingMacro/Release/SparklingMacro is actually installed.
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'SWIFT_COMPILATION_MODE' => 'wholemodule',
    'OTHER_SWIFT_FLAGS' => "-enable-experimental-feature SymbolLinkageMarkers \
        -Xfrontend -load-plugin-executable -Xfrontend ${PODS_TARGET_SRCROOT}/../SparklingMacro/Release/SparklingMacro#SparklingMacrosImpl"
  }
  
  s.subspec 'Application' do |application|
    application.source_files = 'Sources/Application/**/*.{swift,m,h}'
    application.dependency 'Sparkling/Service'
    application.dependency 'Sparkling/Utils'
    application.dependency 'SparklingMethod/Core', s.version.to_s
    application.dependency 'SnapKit'
    application.dependency 'SparklingMacro/RegisterMacro'
    application.resource_bundle = {
      'sparklingPageResource' => 'Sources/Application/Container/UI/SparklingPageResource.xcassets'
    }
    
  end
  
  s.subspec 'Service' do |service|
    service.source_files = 'Sources/Service/{Base,Protocols}/**/*.{swift,m,h}'
    service.dependency 'Sparkling/Utils'
    service.dependency 'SparklingMethod/Core', s.version.to_s
    
    service.subspec 'LynxService' do |lynx|
      lynx.dependency 'Lynx/Framework', '3.6.0'
      lynx.dependency 'LynxBase/Framework', '3.6.0'
      lynx.dependency 'LynxServiceAPI', '3.6.0'
      lynx.dependency 'SparklingMethod/Lynx', s.version.to_s
      lynx.dependency 'SparklingMacro/RegisterMacro'
      lynx.source_files = [
        'Sources/Service/{Base,Protocols}/**/*.{swift,m,h}',
        'Sources/Service/LynxService/**/*.{swift,m,h}',
      ]
    end
  end
  
  s.subspec 'Utils' do |utils|
    utils.source_files = 'Sources/Utils/**/*.{swift,m,mm,h}'
  end
end
