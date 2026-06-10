# Echo Music v5.1.9

* Switched from ffmpeg-kit-full to ffmpeg-kit-audio to reduce native library overhead.
* Fixed ABI filters for individual architectural builds (arm64, armeabi, x86, x86_64) to prevent them from building universal APKs.
* Removed unused aria2c native dependency from YouTube downloader to further reduce app size.