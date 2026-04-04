package com.cms.service;

import com.cms.entity.MailAccount;

public interface IMailTestService {

  /**
   * Verilen {@link MailAccount} hesabını kullanarak {@code testTo} adresine
   * gerçek bir SMTP bağlantısı açar ve deneme maili gönderir.
   *
   * @throws com.cms.exception.BadRequestException SMTP bağlantısı veya gönderim başarısız olursa
   */
  void sendTestEmail(MailAccount account, String testTo);
}
