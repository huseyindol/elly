# OAuth2 Third-Party Authentication Kurulumu

Bu dokümantasyon, Google, Facebook, GitHub ve X (Twitter) gibi third-party OAuth2 authentication'ın nasıl kullanılacağını açıklar.

## Özellikler

- ✅ Google OAuth2
- ✅ Facebook OAuth2
- ✅ GitHub OAuth2
- ✅ X (Twitter) OAuth2 (Not: X OAuth2.0 desteği sınırlı olabilir)

## Kurulum Adımları

### 1. OAuth2 Provider'lardan Client ID ve Secret Alma

#### Google OAuth2
1. [Google Cloud Console](https://console.cloud.google.com/)'a gidin
2. Yeni bir proje oluşturun veya mevcut projeyi seçin
3. "APIs & Services" > "Credentials" bölümüne gidin
4. "Create Credentials" > "OAuth client ID" seçin
5. Application type: "Web application" seçin
6. Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google` ekleyin
7. Client ID ve Client Secret'ı kopyalayın

#### Facebook OAuth2
1. [Facebook Developers](https://developers.facebook.com/)'a gidin
2. Yeni bir App oluşturun
3. "Settings" > "Basic" bölümüne gidin
4. Valid OAuth Redirect URIs: `http://localhost:8080/login/oauth2/code/facebook` ekleyin
5. App ID ve App Secret'ı kopyalayın

#### GitHub OAuth2
1. GitHub hesabınıza gidin
2. Settings > Developer settings > OAuth Apps
3. "New OAuth App" butonuna tıklayın
4. Authorization callback URL: `http://localhost:8080/login/oauth2/code/github` ekleyin
5. Client ID ve Client Secret'ı kopyalayın

### 2. Application Properties Konfigürasyonu

`application.properties` dosyasına OAuth2 bilgilerinizi ekleyin:

```properties
# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET

# Facebook OAuth2
spring.security.oauth2.client.registration.facebook.client-id=YOUR_FACEBOOK_APP_ID
spring.security.oauth2.client.registration.facebook.client-secret=YOUR_FACEBOOK_APP_SECRET

# GitHub OAuth2
spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.client-secret=YOUR_GITHUB_CLIENT_SECRET
```

**Veya environment variables kullanarak:**

```bash
export GOOGLE_CLIENT_ID=your_google_client_id
export GOOGLE_CLIENT_SECRET=your_google_client_secret
export FACEBOOK_CLIENT_ID=your_facebook_app_id
export FACEBOOK_CLIENT_SECRET=your_facebook_app_secret
export GITHUB_CLIENT_ID=your_github_client_id
export GITHUB_CLIENT_SECRET=your_github_client_secret
```

### 3. Frontend Redirect URI

Frontend uygulamanızda redirect URI'yi ayarlayın:

```properties
app.oauth2.redirect-uri=http://localhost:3000/oauth2/redirect
```

Production için:
```properties
app.oauth2.redirect-uri=https://yourdomain.com/oauth2/redirect
```

## Kullanım

### Backend Endpoint'leri

Spring Security otomatik olarak aşağıdaki endpoint'leri oluşturur:

- **Google**: `http://localhost:8080/oauth2/authorization/google`
- **Facebook**: `http://localhost:8080/oauth2/authorization/facebook`
- **GitHub**: `http://localhost:8080/oauth2/authorization/github`

### Frontend Entegrasyonu

Frontend'den OAuth2 login için:

```javascript
// Google ile giriş
window.location.href = 'http://localhost:8080/oauth2/authorization/google';

// Facebook ile giriş
window.location.href = 'http://localhost:8080/oauth2/authorization/facebook';

// GitHub ile giriş
window.location.href = 'http://localhost:8080/oauth2/authorization/github';
```

### Redirect Handler

OAuth2 başarılı olduğunda, kullanıcı şu URL'e yönlendirilir:

```
http://localhost:3000/oauth2/redirect?token=JWT_TOKEN&refreshToken=REFRESH_TOKEN&userId=USER_ID&username=USERNAME&email=EMAIL
```

Frontend'de bu parametreleri alıp kullanabilirsiniz:

```javascript
// React örneği
const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');
const refreshToken = urlParams.get('refreshToken');
const userId = urlParams.get('userId');
const username = urlParams.get('username');
const email = urlParams.get('email');

// Token'ları localStorage'a kaydedin
localStorage.setItem('token', token);
localStorage.setItem('refreshToken', refreshToken);
```

## Veritabanı Değişiklikleri

User entity'sine şu alanlar eklendi:

- `provider`: "local", "google", "facebook", "github", "x"
- `providerId`: OAuth provider'dan gelen unique ID
- `password`: Artık nullable (OAuth kullanıcıları için null)

## Önemli Notlar

1. **X/Twitter OAuth2**: X (eski Twitter) OAuth2.0 desteği sınırlıdır. Tam destek için OAuth 1.0a implementasyonu gerekebilir.

2. **Email Kontrolü**: Aynı email adresi farklı provider'lardan gelebilir. Sistem bu durumu otomatik olarak yönetir ve mevcut kullanıcıya provider bilgisini ekler.

3. **Password**: OAuth kullanıcıları için password null olabilir. Bu durumda normal login yapılamaz, sadece OAuth ile giriş yapılabilir.

4. **Username**: OAuth kullanıcıları için otomatik username oluşturulur. Eğer username zaten varsa, sonuna sayı eklenir (örn: `username_1`, `username_2`).

## Test Etme

1. Uygulamayı başlatın
2. Browser'da şu URL'e gidin: `http://localhost:8080/oauth2/authorization/google`
3. Google login sayfasına yönlendirileceksiniz
4. Giriş yaptıktan sonra frontend redirect URI'nize yönlendirileceksiniz
5. URL'deki token parametrelerini kontrol edin

## Sorun Giderme

### "Invalid redirect URI" hatası
- Provider console'unda redirect URI'nin doğru olduğundan emin olun
- `http://localhost:8080/login/oauth2/code/{provider}` formatında olmalı

### "Client authentication failed" hatası
- Client ID ve Client Secret'ın doğru olduğundan emin olun
- Environment variables kullanıyorsanız, uygulamayı yeniden başlatın

### Token alınamıyor
- Provider console'unda OAuth2 scope'larının doğru olduğundan emin olun
- Application properties'teki redirect URI'nin doğru olduğundan emin olun

