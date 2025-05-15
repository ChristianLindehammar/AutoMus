#!/bin/zsh
# Generate Apple Music Developer Token
# Uses openssl and standard tools available on macOS to generate a JWT token
# for Apple Music API authentication.

# Instructions:
# 1. Get your private key (.p8 file) from Apple Developer Portal
# 2. Get your Team ID from Apple Developer Portal
# 3. Get your Key ID from Apple Developer Portal when you created the key
# 4. Run this script with those values

if [ "$#" -lt 3 ]; then
    echo "Usage: $0 /path/to/p8/file key_id team_id [token_validity_seconds]"
    echo "Example: $0 /Users/name/AuthKey_ABC123.p8 ABC123 TEAMID123 15777000"
    exit 1
fi

# Parse arguments
P8_FILE="$1"
KEY_ID="$2"
TEAM_ID="$3"
VALID_SECONDS=${4:-15777000}  # Default to 6 months in seconds as per Apple's docs

if [ ! -f "$P8_FILE" ]; then
    echo "Error: P8 file not found at $P8_FILE"
    exit 1
fi

# Create a temporary directory
TEMP_DIR=$(mktemp -d)
cd "$TEMP_DIR"

# Get current time and expiry time
ISSUED_AT=$(date +%s)
EXPIRY_TIME=$((ISSUED_AT + VALID_SECONDS))

# Create header
cat > header.json << EOF
{
  "alg": "ES256",
  "kid": "$KEY_ID"
}
EOF

# Create payload
cat > payload.json << EOF
{
  "iss": "$TEAM_ID",
  "iat": $ISSUED_AT,
  "exp": $EXPIRY_TIME
}
EOF

# Base64url encode header and payload
# For macOS compatibility, we use this approach
base64url() {
    # Encode to base64, remove trailing newlines, replace characters and padding
    base64 | tr -d '\n' | tr '+/' '-_' | tr -d '='
}

HEADER_B64=$(cat header.json | base64url)
PAYLOAD_B64=$(cat payload.json | base64url)

# Create the signing input
SIGNING_INPUT="${HEADER_B64}.${PAYLOAD_B64}"
echo -n "$SIGNING_INPUT" > signing_input.txt

# Sign with the private key using ES256 algorithm
SIGNATURE=$(openssl dgst -sha256 -sign "$P8_FILE" signing_input.txt | base64url)

# Create the JWT token
JWT="${SIGNING_INPUT}.${SIGNATURE}"

# Output the token
echo "Generated Developer Token (valid for $(($VALID_SECONDS/86400)) days):"
echo "$JWT"

# Clean up
rm -rf "$TEMP_DIR"

echo ""
echo "Add this token to your local.properties file as:"
echo "APPLE_MUSIC_DEVELOPER_TOKEN=$JWT"
