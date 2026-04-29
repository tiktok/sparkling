Pod::Spec.new do |s|
  s.name           = 'SparklingMacro'
  s.version        = "2.1.0-rc.12"
  s.summary        = "Swift macro plugin for Sparkling Framework"
  s.description    = "Provides the #spk_register Swift compiler macro used by the Sparkling iOS SDK."
  s.license        = "Apache 2.0"
  s.author         = "junchen.ge"
  s.homepage       = 'https://github.com/tiktok/sparkling.git'
  s.platforms      = {
    :ios => '12.0'
  }
  s.swift_version  = '5.10'
  s.source         = { git: 'https://github.com/tiktok/sparkling.git', tag: s.version.to_s }
  s.static_framework = true

  s.default_subspec = 'RegisterMacro'

  s.subspec 'RegisterMacro' do |register_macro|
    register_macro.source_files = 'Sources/RegisterMacro/**/*.{swift,m,mm,h}'
  end
  
  s.preserve_paths = 'Release/SparklingMacro'

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'SWIFT_COMPILATION_MODE' => 'wholemodule',
    'OTHER_SWIFT_FLAGS' => '-Xfrontend -load-plugin-executable -Xfrontend ${PODS_TARGET_SRCROOT}/Release/SparklingMacro#SparklingMacrosImpl'
  }
end
