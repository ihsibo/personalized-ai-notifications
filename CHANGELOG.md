# Changelog

All notable changes to the NotificationAI library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-11-20

### Added
- Initial release of NotificationAI library
- Core `NotificationAI` object with init and generate methods
- OpenAI GPT-3.5-turbo integration via `OpenAIClient`
- Support for 6 different notification tones (Friendly, Motivating, Playful, Empathetic, Humorous, Professional)
- Multi-language notification generation
- A/B testing with multiple variant generation
- Crash-aware notifications
- User session management for personalization
- Frequency settings (Daily, Weekly, Adaptive, Custom)
- Both callback and coroutine-based async APIs
- Comprehensive data models:
  - `NotificationConfig`
  - `NotificationTone`
  - `FrequencySettings`
  - `NotificationResult` (Success/Error)
  - OpenAI request/response models
- `PromptBuilder` for dynamic prompt generation
- Demo app with UI showcasing library features
- Comprehensive documentation (README.md)
- OkHttp for HTTP networking
- Gson for JSON serialization
- Kotlin Coroutines support

### Security
- Secure API key handling
- No hardcoded credentials
- BuildConfig integration guidelines

### Documentation
- Main README with quick start guide
- Library-specific README with detailed API reference
- Code examples for common use cases
- Security best practices
- Cost optimization tips
- MIT License

## [Unreleased]

### Planned Features
- Support for Claude and Gemini AI models
- Built-in analytics and metrics
- Notification template library
- Rate limiting utilities
- Caching mechanisms
- Firebase Cloud Messaging integration
- Notification scheduling
- WordPress plugin
- Performance optimizations
- Extended language support

---

## Version History

- **1.0.0** (2025-11-20) - Initial release

---

## Contributing

See [README.md](README.md) for contribution guidelines.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

