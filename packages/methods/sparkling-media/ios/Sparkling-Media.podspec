Pod::Spec.new do |s|
  s.name           = 'Sparkling-Media'
  s.version        = "2.1.0-rc.12"
  s.summary        = "Sparkling media methods for choosing, uploading, and downloading media files"
  s.description    = "Sparkling media methods for choosing, uploading, and downloading media files"
  s.license        = "Apache-2.0"
  s.author         = "TikTok"
  s.homepage       = 'https://github.com/tiktok/sparkling'
  s.platforms      = {
    :ios => '12.0'
  }
  s.swift_version  = '5.7'
  s.source         = { git: 'https://github.com/tiktok/sparkling.git', tag: s.version.to_s, path: 'packages/methods/sparkling-media/ios' }
  s.static_framework = true

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'SWIFT_COMPILATION_MODE' => 'wholemodule'
  }

  s.subspec 'Core' do |core|
    core.source_files = [
      'Sources/Core/Methods/**/*.{h,m,swift}',
      'Sources/Core/Utils/*.{h,m,swift}',
    ]
  end

  s.dependency 'SparklingMethod/Core'
  s.dependency 'SparklingMethod/DIProvider'
  s.dependency 'Mantle', '~> 2.2.0'
end
