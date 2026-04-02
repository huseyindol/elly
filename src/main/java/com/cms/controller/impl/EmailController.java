package com.cms.controller.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.IEmailController;
import com.cms.dto.DtoEmailLog;
import com.cms.dto.EmailRequest;
import com.cms.entity.RootEntityResponse;
import com.cms.service.IEmailService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController implements IEmailController {

  private final IEmailService emailService;

  @PostMapping("/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Override
  public RootEntityResponse<DtoEmailLog> sendEmail(@Valid @RequestBody EmailRequest request) {
    DtoEmailLog result = emailService.sendEmail(request);
    RootEntityResponse<DtoEmailLog> response = new RootEntityResponse<>();
    response.setResult(true);
    response.setMessage("Mail kuyruğa alındı, ID: " + result.getId());
    response.setData(result);
    return response;
  }

  @GetMapping("/templates")
  @Override
  public RootEntityResponse<List<String>> getAvailableTemplates() {
    return RootEntityResponse.ok(emailService.getAvailableTemplates());
  }
}
