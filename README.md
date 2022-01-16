Radio Recorder
===================
[![Circle CI Status](https://img.shields.io/circleci/build/github/sfuhrm/radiorecorder?style=plastic)](https://app.circleci.com/pipelines/github/sfuhrm/radiorecorder)
[![Dependency Check](https://github.com/sfuhrm/radiorecorder/actions/workflows/dependency-check.yml/badge.svg)](https://github.com/sfuhrm/radiorecorder/actions/workflows/dependency-check.yml)
[![Integration Test](https://github.com/sfuhrm/radiorecorder/actions/workflows/maven-integration.yml/badge.svg)](https://github.com/sfuhrm/radiorecorder/actions/workflows/maven-integration.yml)
[![ReleaseDate](https://img.shields.io/github/release-date/sfuhrm/radiorecorder)](https://github.com/sfuhrm/radiorecorder/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A command line internet radio player and recorder.
Some of the features are:
* Display of the current song title played.
* Live playback using Java Media Framework.
* Live Google Chromecast playback of radio streams.
* Recording of one file per song.
* Writing of ID3 tags (ID3v1 and ID3V2.4).
* Parallel recording of multiple radio stations.
* Integrated querying and resolving using the [Radio Browser](https://www.radio-browser.info/) internet radio database.

## Downloading & installation

The current version can be downloaded for Debian, CentOS and Windows systems here:

https://github.com/sfuhrm/radiorecorder/releases

The Debian package can be downloaded and installed like this:

```shell
wget https://github.com/sfuhrm/radiorecorder/releases/download/radiorecorder-1.4.0/radiorecorder_1.4.0-1_amd64.deb
apt install ./radiorecorder_1.4.0-1_amd64.deb
```

after that, the executable is in `/opt/radiorecorder/bin/radiorecorder`.

## Usage

### Command line options

The program is a command line only program. It supports multiple parameters:

```
 URLORNAME                              : URLs of the internet radio station(s)
                                          or station name for lookup at
                                          http://www.radio-browser.info/
 -abort-after KB                        : Abort after writing the given amount
                                          of kilobytes to target drive.
 -cast (-c) VAL                         : Stream to the given chrome cast
                                          device.
 -client (-C) [JAVA_NET |               : Specify HTTP client to use. (default:
 APACHE_CLIENT_4 | APACHE_CLIENT_5]       APACHE_CLIENT_5)
 -directory (-d) DIR                    : Write to this directory. (default:
                                          /home/fury)
 -help (-h)                             : Show this command line help.
                                          (default: false)
 -limit (-l) COUNT                      : Limit of stations to download in
                                          parallel. (default: 10)
 -list-cast (-L)                        : List chromecast devices. (default:
                                          false)
 -min-free (-M) MEGS                    : Minimum of free megs on target drive.
                                          (default: 512)
 -play (-p)                             : Play live instead of recording to a
                                          file. (default: false)
 -reconnect (-r)                        : Automatically reconnect after
                                          connection loss. (default: false)
 -timeout (-T) SECS                     : Connect/read timeout in seconds.
                                          (default: 60)
 -use-songnames (-S)                    : Use songnames from retrieved metadata
                                          information. Will create one file per
                                          detected song. (default: false)
```

### Debian package

The installed executable can be executed like this:

```shell
/opt/radiorecorder/bin/radiorecorder -p synthradio
```

## License

Copyright 2017-2021 Stephan Fuhrmann

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
