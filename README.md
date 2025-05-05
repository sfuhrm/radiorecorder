Radio Recorder
===================
[![Java CI with Maven](https://github.com/sfuhrm/radiorecorder/actions/workflows/maven-ref.yml/badge.svg)](https://github.com/sfuhrm/radiorecorder/actions/workflows/maven-ref.yml)
[![Integration Test](https://github.com/sfuhrm/radiorecorder/actions/workflows/maven-integration.yml/badge.svg)](https://github.com/sfuhrm/radiorecorder/actions/workflows/maven-integration.yml)
[![ReleaseDate](https://img.shields.io/github/release-date/sfuhrm/radiorecorder)](https://github.com/sfuhrm/radiorecorder/releases)
![Maven Central](https://img.shields.io/maven-central/v/de.sfuhrm/radiorecorder)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A command line internet radio player and recorder.

Some of the features are:
* Live Playback
  * Display of the current song title played.
  * Live playback using Java Media Framework.
  * Live Google Chromecast playback of radio streams.
* Recording
  * Recording of one file per song.
  * Writing of ID3 tags (ID3v1 and ID3V2.4).
  * Parallel recording of multiple radio stations.
* Integrated querying and resolving using the [Radio Browser](https://www.radio-browser.info/) internet radio database.
* Stream formats:
  * MP3
  * OGG Vorbis
  * AAC/AAC+/MP4

## Downloading & installation

The current version can be downloaded for Debian, CentOS and Mac OS X systems here:

https://github.com/sfuhrm/radiorecorder/releases

### Releases

Releases typically contain files named like this for downloading:

* Shipped as x86/AMD64 installable package
  * `radiorecorder_xxx_amd64.deb`: A Debian install archive with the program and the Java runtime included. 
  * `radiorecorder-xxx.x86_64.rpm`: A CentOS/RHEL install archive with the program and the Java runtime included. 
  * `radiorecorder-xxx.pkg`: A macOS PKG installer with the program and the Java runtime included.
  * `radiorecorder-xxx.dmg`: A macOS DMG installer with the program and the Java runtime included.
* Needing a JDK, runnable on any platform
  * `radiorecorder-xxx-bin.tar.bz2`: A program-only archive in tar.bz2 format that requires a Java runtime installed on your system.
  * `radiorecorder-xxx-bin.tar.gz`: A program-only archive in tar.gz format that requires a Java runtime installed on your system.
  * `radiorecorder-xxx-bin.zip`: A program-only archive in ZIP format that requires a Java runtime installed on your system.

You can choose whether you prefer an installation with the runtime as a package or care for the Java runtime yourself.
The latter makes sense when you're on Windows, Aarch64, X86, or some other system with no dedicated installer.

## Usage

### Playback

When playing, the usage can consist of the search and the play step:

* Search the stations (i.e. `radiorecorder -list-station synthpop`)
* Play a specific radio station of the list from above (`radiorecorder -play f9ab3256-33a7-41a3-ba57-646bf3750ae9`)

### Recording

When recording, the usage usually consists also of the two steps:

* Search the stations (i.e. `radiorecorder -list-station synthpop`)
* Record a specific radio station of the list from above (`radiorecorder -use-songnames -directory . f9ab3256-33a7-41a3-ba57-646bf3750ae9`)

### Command line options

The program is a command line only program. It supports multiple parameters:

```
 URL_OR_UUID_OR_NAME                    : URLs of the internet radio
                                          station(s), (partial) station name
                                          for lookup or the station UUID (see
                                          option -list-station)
 -abort-after-duration DURATION         : Abort after a given time, i.e.
                                          '3m10s', '3h' or '10s'.
 -abort-after-kb (-abort-after) KB      : Abort after writing the given amount
                                          of kilobytes to target drive.
 -cast (-c) CASTDEVICE_TITLE            : Stream to the given chrome cast
                                          device. Use cast device title from
                                          '-list-cast'.
 -client (-C) [JAVA_NET |               : Specify HTTP client to use. (default:
 APACHE_CLIENT_5]                         APACHE_CLIENT_5)
 -directory (-d) DIR                    : Write recorded stream files to a
                                          folder hierarchy in this target
                                          directory.
 -help (-h)                             : Show this command line help.
                                          (default: true)
 -limit (-l) COUNT                      : Limit of stations to download in
                                          parallel. (default: 10)
 -list-cast (-L)                        : List chromecast devices, then exit.
                                          (default: false)
 -list-mixer (-X)                       : List audio playback mixers, then
                                          exit. (default: false)
 -list-station (-Z)                     : List matching radio stations limited
                                          by '-limit', then exit. (default:
                                          false)
 -min-free (-M) MEGS                    : Minimum of free megs on target drive.
                                          (default: 512)
 -mixer (-m) MIXER_NAME                 : The mixer to use for playback. The
                                          mixer parameter is the name from the
                                          '-list-mixer' option output.
 -play (-p)                             : Play live instead of recording to a
                                          file. (default: false)
 -proxy (-P) URL                        : The HTTP/HTTPS proxy to use.
 -reconnect (-r)                        : Automatically reconnect after
                                          connection loss. (default: false)
 -timeout (-T) SECS                     : Connect/read timeout in seconds.
                                          (default: 60)
 -use-songnames (-S)                    : Use songnames from retrieved metadata
                                          information. Will create one file per
                                          detected song. (default: false)
 -version (-V)                          : Show version information and exit.
                                          (default: false)
```

## License

Copyright 2017-2024 Stephan Fuhrmann

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
