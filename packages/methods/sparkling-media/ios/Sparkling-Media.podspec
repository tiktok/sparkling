Pod::Spec.new do |s|
  s.name = 'Sparkling-Media'
  s.version = '2.1.0-rc.12'
  s.summary = 'Media methods for SparklingMethod'
  s.license = 'Apache-2.0'
  s.author = 'TikTok'
  s.homepage = 'https://github.com/tiktok/sparkling'
  s.platforms = { :ios => '14.0' }
  s.swift_version = '5.10'
  s.source = { :git => 'https://github.com/tiktok/sparkling.git', :tag => s.version.to_s }
  s.static_framework = true
  s.source_files = 'Sources/Core/**/*.swift'
  s.dependency 'SparklingMethod/Macros', s.version.to_s
  macro_binary = '$(SPK_METHOD_MACRO_PLUGIN)'
  s.pod_target_xcconfig = {
    'SPK_METHOD_MACRO_PLUGIN' => '${PODS_ROOT}/SparklingMethod/Release/SparklingMethodMacroPlugin',
    'OTHER_SWIFT_FLAGS' => "-enable-experimental-feature SymbolLinkageMarkers -D_SYMBOL_LINKAGE_MARKERS_ENABLED -Xfrontend -load-plugin-executable -Xfrontend #{macro_binary}#SparklingMethodMacroPlugin"
  }
end
