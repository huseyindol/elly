package com.cms.config;

/**
 * Tüm permission string sabitlerini tanımlar.
 * Controller'lardaki @PreAuthorize annotation'larında kullanılır.
 * Format: "modül:işlem"
 */
public final class PermissionConstants {

  private PermissionConstants() {
    // Utility class
  }

  // =============== POSTS ===============
  public static final String POSTS_CREATE = "posts:create";
  public static final String POSTS_READ = "posts:read";
  public static final String POSTS_UPDATE = "posts:update";
  public static final String POSTS_DELETE = "posts:delete";

  // =============== PAGES ===============
  public static final String PAGES_CREATE = "pages:create";
  public static final String PAGES_READ = "pages:read";
  public static final String PAGES_UPDATE = "pages:update";
  public static final String PAGES_DELETE = "pages:delete";

  // =============== COMPONENTS ===============
  public static final String COMPONENTS_CREATE = "components:create";
  public static final String COMPONENTS_READ = "components:read";
  public static final String COMPONENTS_UPDATE = "components:update";
  public static final String COMPONENTS_DELETE = "components:delete";

  // =============== WIDGETS ===============
  public static final String WIDGETS_CREATE = "widgets:create";
  public static final String WIDGETS_READ = "widgets:read";
  public static final String WIDGETS_UPDATE = "widgets:update";
  public static final String WIDGETS_DELETE = "widgets:delete";

  // =============== BANNERS ===============
  public static final String BANNERS_CREATE = "banners:create";
  public static final String BANNERS_READ = "banners:read";
  public static final String BANNERS_UPDATE = "banners:update";
  public static final String BANNERS_DELETE = "banners:delete";

  // =============== ASSETS ===============
  public static final String ASSETS_CREATE = "assets:create";
  public static final String ASSETS_READ = "assets:read";
  public static final String ASSETS_UPDATE = "assets:update";
  public static final String ASSETS_DELETE = "assets:delete";

  // =============== COMMENTS ===============
  public static final String COMMENTS_CREATE = "comments:create";
  public static final String COMMENTS_READ = "comments:read";
  public static final String COMMENTS_DELETE = "comments:delete";

  // =============== FORMS ===============
  public static final String FORMS_CREATE = "forms:create";
  public static final String FORMS_READ = "forms:read";
  public static final String FORMS_UPDATE = "forms:update";
  public static final String FORMS_DELETE = "forms:delete";

  // =============== RATINGS ===============
  public static final String RATINGS_CREATE = "ratings:create";
  public static final String RATINGS_READ = "ratings:read";

  // =============== CONTENTS ===============
  public static final String CONTENTS_CREATE = "contents:create";
  public static final String CONTENTS_READ = "contents:read";
  public static final String CONTENTS_UPDATE = "contents:update";
  public static final String CONTENTS_DELETE = "contents:delete";

  // =============== BASIC_INFOS ===============
  public static final String BASIC_INFOS_CREATE = "basic_infos:create";
  public static final String BASIC_INFOS_READ = "basic_infos:read";
  public static final String BASIC_INFOS_UPDATE = "basic_infos:update";
  public static final String BASIC_INFOS_DELETE = "basic_infos:delete";

  // =============== MAIL ===============
  public static final String MAIL_CREATE = "mail:create";
  public static final String MAIL_READ = "mail:read";
  public static final String MAIL_UPDATE = "mail:update";
  public static final String MAIL_DELETE = "mail:delete";

  // =============== EMAILS ===============
  public static final String EMAILS_SEND = "emails:send";
  public static final String EMAILS_READ = "emails:read";
  public static final String EMAILS_RETRY = "emails:retry";

  // =============== CACHE ===============
  public static final String CACHE_READ = "cache:read";
  public static final String CACHE_MANAGE = "cache:manage";

  // =============== TENANTS ===============
  public static final String TENANTS_READ = "tenants:read";
  public static final String TENANTS_MANAGE = "tenants:manage";

  // =============== USERS ===============
  public static final String USERS_READ = "users:read";
  public static final String USERS_UPDATE = "users:update";
  public static final String USERS_MANAGE = "users:manage";

  // =============== ROLES ===============
  public static final String ROLES_READ = "roles:read";
  public static final String ROLES_CREATE = "roles:create";
  public static final String ROLES_UPDATE = "roles:update";
  public static final String ROLES_DELETE = "roles:delete";

  // =============== RABBITMQ ADMIN ===============
  public static final String RABBIT_READ = "rabbit:read";
  public static final String RABBIT_MANAGE = "rabbit:manage";

  // =============== EMAIL TEMPLATES (v4) ===============
  public static final String EMAIL_TEMPLATES_READ = "email_templates:read";
  public static final String EMAIL_TEMPLATES_MANAGE = "email_templates:manage";
}
