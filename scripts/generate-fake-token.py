#!/usr/bin/env python3
"""
Fake/Geçersiz JWT Access Token Oluşturucu

Bu script, test amaçlı geçersiz/eskimiş JWT access token'ları oluşturur.
JWE (JSON Web Encryption) formatında token üretir.

Kullanım:
    python3 scripts/generate-fake-token.py

veya

    chmod +x scripts/generate-fake-token.py
    ./scripts/generate-fake-token.py
"""

import base64
import json
import sys
import os

def generate_fake_token():
    """
    Geçersiz bir JWE token oluşturur.
    JWE formatı: header.encrypted_key.iv.ciphertext.tag
    """
    # Header: {"alg":"dir","enc":"A256GCM"}
    header = {"alg": "dir", "enc": "A256GCM"}
    encoded_header = base64.urlsafe_b64encode(
        json.dumps(header).encode()
    ).decode().rstrip('=')

    # Fake encrypted key (geçersiz)
    fake_encrypted_key = "fake_encrypted_key_12345678901234567890"
    encoded_key = base64.urlsafe_b64encode(
        fake_encrypted_key.encode()
    ).decode().rstrip('=')

    # Fake IV (Initialization Vector)
    fake_iv = "fake_iv_123456789012345678901234"
    encoded_iv = base64.urlsafe_b64encode(
        fake_iv.encode()
    ).decode().rstrip('=')

    # Fake ciphertext (şifrelenmiş veri)
    fake_ciphertext = "fake_ciphertext_data_that_is_invalid_and_will_fail_decryption"
    encoded_ciphertext = base64.urlsafe_b64encode(
        fake_ciphertext.encode()
    ).decode().rstrip('=')

    # Fake tag (authentication tag)
    fake_tag = "fake_tag_123456789012345678901234"
    encoded_tag = base64.urlsafe_b64encode(
        fake_tag.encode()
    ).decode().rstrip('=')

    # JWE format: header.encrypted_key.iv.ciphertext.tag
    fake_token = f"{encoded_header}.{encoded_key}.{encoded_iv}.{encoded_ciphertext}.{encoded_tag}"

    return fake_token


def generate_simple_fake_token():
    """
    Daha basit bir geçersiz token oluşturur (JWT formatında ama geçersiz).
    """
    # Fake JWT header
    header = {"alg": "HS256", "typ": "JWT"}
    encoded_header = base64.urlsafe_b64encode(
        json.dumps(header).encode()
    ).decode().rstrip('=')

    # Fake payload
    payload = "invalid_payload_data"
    encoded_payload = base64.urlsafe_b64encode(
        payload.encode()
    ).decode().rstrip('=')

    # Fake signature
    signature = "invalid_signature_12345"
    encoded_signature = base64.urlsafe_b64encode(
        signature.encode()
    ).decode().rstrip('=')

    # JWT format: header.payload.signature
    fake_token = f"{encoded_header}.{encoded_payload}.{encoded_signature}"

    return fake_token


def main():
    """Ana fonksiyon"""
    print("=" * 80)
    print("FAKE/INVALID ACCESS TOKEN GENERATOR")
    print("=" * 80)
    print()

    # JWE formatında fake token
    print("1. JWE Formatında Geçersiz Token:")
    print("-" * 80)
    jwe_token = generate_fake_token()
    print(jwe_token)
    print()
    print(f"Token Length: {len(jwe_token)} characters")
    print()

    # Basit JWT formatında fake token
    print("2. Basit JWT Formatında Geçersiz Token:")
    print("-" * 80)
    jwt_token = generate_simple_fake_token()
    print(jwt_token)
    print()
    print(f"Token Length: {len(jwt_token)} characters")
    print()

    # Test komutları
    print("=" * 80)
    print("TEST KOMUTLARI")
    print("=" * 80)
    print()
    print("JWE Token ile test:")
    print(f"curl -X 'GET' 'http://localhost:8080/api/v1/pages/home' \\")
    print(f"  -H 'accept: */*' \\")
    print(f"  -H 'Authorization: Bearer {jwe_token}'")
    print()
    print("Basit JWT Token ile test:")
    print(f"curl -X 'GET' 'http://localhost:8080/api/v1/pages/home' \\")
    print(f"  -H 'accept: */*' \\")
    print(f"  -H 'Authorization: Bearer {jwt_token}'")
    print()
    print("=" * 80)
    print("BEKLENEN SONUÇ:")
    print("=" * 80)
    print("""
{
  "result": false,
  "status": 401,
  "error": "Unauthorized",
  "errorCode": "BAD_CREDENTIALS",
  "message": "Invalid or expired token"
}
    """)
    print("=" * 80)


if __name__ == "__main__":
    main()
