[![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

# Euro-Office Documents for Android

Euro-Office Documents is a free office app for Android. It opens, edits and co-edits documents, spreadsheets, presentations and PDF forms, on the device and in Nextcloud.

This repository is the app shell: file manager, accounts, storages and sharing. The editors are in [editors-android](https://github.com/Euro-Office/editors-android), and the whole app is built from [AndroidEditors](https://github.com/Euro-Office/AndroidEditors).

## Features

- **Documents, spreadsheets, presentations:** native editors on top of the Euro-Office document engine
- **PDF:** view, annotate and fill in forms
- **On your device:** open and edit local files, also offline
- **Nextcloud:** browse your Nextcloud and co-edit its documents in real time through the Euro-Office connector

The app is a fork of ONLYOFFICE Documents for Android. Support for other clouds (ownCloud, kDrive, WebDAV, OneDrive, Dropbox, Google Drive, portals) is still in the code but hidden (`AppConfig.isNextcloudOnly`).

## Build

See [AndroidEditors](https://github.com/Euro-Office/AndroidEditors): all builds run in Docker.

## Issues

Please report problems at [Euro-Office](https://github.com/Euro-Office).

## License

Euro-Office Documents for Android is released under the AGPLv3 license. See [LICENSE.txt](LICENSE.txt).

It is based on [ONLYOFFICE Documents for Android](https://github.com/ONLYOFFICE/documents-app-android) 9.3.1 (707) by Ascensio System SIA, released under the AGPLv3, and has been modified by the Euro-Office contributors since October 2026 (see the Git history). Changed pictures say so in a comment.

The Euro-Office logo is the Material Symbols icon "description" by Google, under the [Apache License 2.0](LICENSES/Apache-2.0.txt). [REUSE.toml](REUSE.toml) gives the licence of each file; the third-party libraries are listed in [thirdparty.md](thirdparty.md).
