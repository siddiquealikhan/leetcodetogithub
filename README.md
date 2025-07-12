# LeetCode to GitHub Uploader

A Java application that automatically monitors LeetCode submissions and uploads accepted solutions to a GitHub repository. This tool helps developers maintain an organized collection of their LeetCode solutions by automatically syncing them to GitHub.

## Overview

The LeetCode to GitHub Uploader is a Maven-based Java application that uses Selenium WebDriver to monitor LeetCode submissions in real-time. When it detects an accepted submission, it automatically extracts the solution code and uploads it to a configured GitHub repository with proper formatting and metadata.

## Features

- Real-time monitoring of LeetCode submissions
- Automatic detection of accepted solutions
- GitHub integration for code uploads
- Configurable monitoring intervals
- Comprehensive logging system
- Headless and visible browser modes
- Support for multiple programming languages
- Automatic file naming and organization

## Architecture

The application follows a modular architecture with the following components:

### Core Components

- **LeetCodeUploader**: Main application class that orchestrates the monitoring process
- **ConfigurationManager**: Handles application configuration and validation
- **LeetCodeMonitorService**: Manages browser automation and submission detection
- **GitHubService**: Handles GitHub API interactions for file uploads
- **Submission**: Data model for representing LeetCode submissions

### Technology Stack

- **Java 17**: Core programming language
- **Maven**: Build and dependency management
- **Selenium WebDriver 4.15.0**: Browser automation
- **OkHttp 4.12.0**: HTTP client for GitHub API
- **Jackson 2.15.3**: JSON processing
- **Logback 1.4.11**: Logging framework
- **Apache Commons**: Configuration and utility libraries

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Chrome browser installed
- ChromeDriver compatible with your Chrome version
- GitHub personal access token
- LeetCode account

## Installation

1. Clone the repository:

   ```bash
   git clone <repository-url>
   cd leetcode-to-github
   ```

2. Build the project:

   ```bash
   mvn clean package
   ```

3. Configure the application by creating a `config.properties` file in the project root:
   ```properties
   github.token=your_github_personal_access_token
   github.repo=https://github.com/yourusername/your-repo-name
   leetcode.username=your_leetcode_username
   logging.level=INFO
   browser.headless=false
   ```

## Configuration

### Required Configuration

- **github.token**: Your GitHub personal access token with repo permissions
- **github.repo**: The full URL of your GitHub repository where solutions will be uploaded
- **leetcode.username**: Your LeetCode username for monitoring submissions

### Optional Configuration

- **logging.level**: Logging level (DEBUG, INFO, WARN, ERROR). Default: INFO
- **browser.headless**: Set to true for headless browser mode. Default: false

### GitHub Token Setup

1. Go to GitHub Settings > Developer settings > Personal access tokens
2. Generate a new token with the following permissions:
   - `repo` (Full control of private repositories)
   - `workflow` (Update GitHub Action workflows)
3. Copy the token and add it to your `config.properties` file

## Usage

### Running the Application

1. Ensure your `config.properties` file is properly configured
2. Run the application:
   ```bash
   java -jar target/leetcode-uploader-1.0.0.jar
   ```

### How It Works

1. The application starts and loads configuration
2. Initializes a Chrome WebDriver instance
3. Navigates to LeetCode and monitors for new submissions
4. When an accepted submission is detected:
   - Extracts the problem information and solution code
   - Formats the code with proper metadata
   - Uploads the file to the configured GitHub repository
   - Logs the operation details

### Monitoring Process

- The application checks for new submissions every 3 seconds
- It monitors the LeetCode submissions page for your account
- Only processes accepted submissions (status: "Accepted")
- Automatically handles browser session management
- Provides detailed logging of all operations

## File Structure

```
leetcode-to-github/
├── config.properties          # Application configuration
├── LICENSE                    # MIT License
├── logs/                      # Application logs
│   └── leetcode-uploader.log
├── src/                       # Source code (compiled)
├── target/                    # Build artifacts
│   ├── classes/               # Compiled classes
│   └── leetcode-uploader-1.0.0.jar
└── .gitignore                 # Git ignore rules
```

## Logging

The application uses Logback for logging with the following configuration:

- **Console Output**: Real-time logging to console
- **File Output**: Rolling log files in the `logs/` directory
- **Log Retention**: 30 days of log history
- **Log Format**: Timestamp, thread, level, logger, and message

### Log Levels

- **INFO**: General application flow and successful operations
- **WARN**: Configuration warnings and non-critical issues
- **ERROR**: Errors that prevent normal operation
- **DEBUG**: Detailed debugging information

## Troubleshooting

### Common Issues

1. **Configuration Errors**

   - Ensure all required properties are set in `config.properties`
   - Verify GitHub token has correct permissions
   - Check repository URL format

2. **Browser Issues**

   - Ensure Chrome browser is installed
   - Update ChromeDriver to match Chrome version
   - Try running in visible mode for debugging

3. **GitHub API Errors**

   - Verify GitHub token is valid and not expired
   - Check repository permissions
   - Ensure repository exists and is accessible

4. **LeetCode Access Issues**
   - Verify LeetCode username is correct
   - Ensure you're logged into LeetCode in the browser
   - Check for any LeetCode site changes

### Debug Mode

To enable debug logging, set the logging level to DEBUG in `config.properties`:

```properties
logging.level=DEBUG
```

## Development

### Building from Source

1. Clone the repository
2. Install dependencies:
   ```bash
   mvn dependency:resolve
   ```
3. Compile the project:
   ```bash
   mvn compile
   ```
4. Run tests:
   ```bash
   mvn test
   ```
5. Package the application:
   ```bash
   mvn package
   ```

### Project Structure

The compiled application includes the following packages:

- `com.leet2hub`: Main application package
- `com.leet2hub.config`: Configuration management
- `com.leet2hub.model`: Data models
- `com.leet2hub.service`: Core services

## Security Considerations

- Never commit your `config.properties` file to version control
- Keep your GitHub token secure and rotate it regularly
- The application stores sensitive configuration locally
- Consider using environment variables for production deployments

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

Siddique Ali Khan

## Support

For issues and questions:

1. Check the troubleshooting section
2. Review the application logs
3. Create an issue in the repository

## Changelog

### Version 1.0.0

- Initial release
- LeetCode submission monitoring
- GitHub integration
- Configuration management
- Comprehensive logging
- Browser automation support
