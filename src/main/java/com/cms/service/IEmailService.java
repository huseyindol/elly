package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoEmailLog;
import com.cms.dto.EmailRequest;
import com.cms.enums.EmailStatus;

public interface IEmailService {

  DtoEmailLog sendEmail(EmailRequest request);

  List<String> getAvailableTemplates();

  /**
   * FAILED ya da takilmis PENDING durumundaki bir EmailLog'u yeniden RabbitMQ'ya
   * publish eder. Status = PENDING, retry_count = 0, error_message = null olarak
   * guncellenir. SENT kayitlarini retry etmez (validation hatasi firlatir).
   */
  DtoEmailLog retry(Long emailLogId);

  /**
   * Panel UI icin: tum email log'lari, opsiyonel status filtresi ile paginated.
   * status null ise tum kayitlar, aksi halde sadece o durumdakiler.
   */
  Page<DtoEmailLog> list(EmailStatus status, Pageable pageable);
}
