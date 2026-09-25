Pod::Spec.new do |s|
  s.name             = 'SparklingMethod'
  s.version          = '2.1.0-rc.12'
  s.summary          = 'Native method runtime for Sparkling'
  s.description      = 'Method registration, invocation, models, and events for Sparkling.'
  s.homepage         = 'https://github.com/tiktok/sparkling'
  s.license          = { :type => 'Apache-2.0', :file => 'LICENSE' }
  s.authors          = 'The Sparkling Authors'
  s.source           = { :git => 'https://github.com/tiktok/sparkling.git', :tag => s.version.to_s }
  s.ios.deployment_target = '14.0'
  s.frameworks       = 'Foundation'
  s.swift_version    = '5.10'
  s.preserve_paths   = 'Release/SparklingMethodMacroPlugin'

  s.subspec 'Core' do |core|
    core.frameworks = 'WebKit'
    core.dependency 'Mantle', '2.2.0'
    core.source_files = 'Sources/SparklingMethod/**/*.{h,m}'
    core.exclude_files = 'Sources/SparklingMethod/Implementation/Transport/Lynx/**/*'
    core.public_header_files = 'Sources/SparklingMethod/include/SparklingMethod/*.h'
    core.private_header_files = 'Sources/SparklingMethod/Implementation/Runtime/Internal/*.h'
  end

  s.subspec 'Lynx' do |lynx|
    lynx.dependency 'SparklingMethod/Core'
    lynx.dependency 'Lynx/Framework', '>= 1.3'
    lynx.source_files = [
      'Sources/SparklingMethod/Implementation/Transport/Lynx/**/*.{h,m}',
      'Sources/SparklingMethodLynxHeaders/*.h'
    ]
    lynx.public_header_files = 'Sources/SparklingMethodLynxHeaders/*.h'
    lynx.private_header_files = 'Sources/SparklingMethod/Implementation/Transport/Lynx/Internal/*.h'
  end

  s.subspec 'Macros' do |macros|
    macros.dependency 'SparklingMethod/Core'
    macros.source_files = 'Sources/SparklingMethodMacros/SPKGlobalMethod.swift'
  end

  s.default_subspecs = 'Core'
end
