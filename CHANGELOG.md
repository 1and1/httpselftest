# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

<!-- Categories: Added, Changed, Deprecated, Removed, Fixed, Security -->

## [Unreleased]
- shade j2html
- render unhandled Throwables to html

## [0.3.1] - 2019-08-29
- added uncaught exception messages to JSON response
- added json presentation

## [0.3.0] - 2019-08-29
- added support for different request presentations
- added x-www-form-urlencoded presentation
- added hex response presentation
- added support for different response presentations

## [0.2.3] - 2019-05-02
- added support for unmodifiable test-provided parameters via `TestConfigs.fixed(..)`
- extended JSON response

## [0.2.2] - 2019-03-28
- fix artifact deployment to sonatype

## [0.2.1] - 2019-03-28
### Added
- Support SHA-256 for Basic auth. Use by prefixing credentials with `sha256|`, e.g. `selftest.credentials=sha256|a60ec1cf58f49cd2fab6d9e...`.

## [0.2.0] - 2019-02-22
### Added
- Dependency on Logback is now optional. Logging framework is configurable under `selftest.logger`, defaulting to logback.

## [0.1.0] - 2019-02-08
### Added
- First release
