# AutoMus - Apple Music for Android Automotive

This application provides an Apple Music experience for Android Automotive systems.

## Authentication Setup

Authentication with Apple Music requires several steps:

1. A developer token generated from your p8 file (private key)
2. User login via OAuth to get a user token

### Setting up Developer Authentication

To use this app, you need:

1. An Apple Developer account with MusicKit access
2. A pre-generated developer token (see below for generation options)

#### Generating the Developer Token

You have two options for generating the developer token:

**Option 1: Using the Shell Script (Recommended)**

The included shell script uses standard Unix tools to create a JWT token:

```bash
./generate_apple_token.sh /path/to/your/AuthKey_KEYID.p8 YOUR_KEY_ID YOUR_TEAM_ID 4380
```

This creates a token valid for 6 months (4380 hours) that you can add to your build configuration.

**Option 2: Using an Online JWT Generator**

If you don't want to handle p8 files locally, you can use an online JWT generator with the following parameters:
- Algorithm: ES256 (ECDSA using P-256 curve and SHA-256 hash)
- Payload:
  ```json
  {
    "iss": "YOUR_TEAM_ID",
    "iat": current_timestamp,
    "exp": expiration_timestamp
  }
  ```
- Headers:
  ```json
  {
    "alg": "ES256",
    "kid": "YOUR_KEY_ID"
  }
  ```

Note: Use a reputable service for generating tokens if you choose this option.

#### Adding the token to your build

Add the generated token to your `local.properties` file:

```properties
APPLE_MUSIC_DEVELOPER_TOKEN=eyJhbGciOiJFUzI1...your token here
APPLE_MUSIC_CLIENT_ID=your.client.id
APPLE_MUSIC_CLIENT_SECRET=your_client_secret_here
```

#### Authentication Flow

Once the app is built with valid credentials:

1. When a user taps the profile icon, they'll see a login prompt if not already authenticated
2. After tapping Login, they'll be directed to Apple's authentication page in a web view
3. After successful login, the app will get a user token which provides access to the user's Apple Music content

### Important Notes

- The p8 file should never be included in the app or repository - it's only used for token generation
- The developer token identifies your app to Apple - all users of your app use the same developer token
- Each user gets their own user token through the OAuth login process
- The simplified implementation uses a pre-generated token stored in BuildConfig, avoiding any need for p8 file handling in the app
- Developer tokens expire after a maximum of 6 months, so plan to update your token periodically

## Building from Source

1. Generate your Apple Music developer token as described above
2. Add credentials to your `local.properties` file
3. Build using Gradle:

```bash
./gradlew assembleRelease
```
