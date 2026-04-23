package com.cms.controller.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cms.config.PermissionConstants;
import com.cms.controller.IRabbitAdminController;
import com.cms.dto.rabbit.DtoRabbitMessage;
import com.cms.dto.rabbit.DtoRabbitOverview;
import com.cms.dto.rabbit.DtoRabbitQueue;
import com.cms.dto.rabbit.RepublishRequest;
import com.cms.entity.RootEntityResponse;
import com.cms.service.IRabbitAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Panel UI'nin RabbitMQ yonetim istekleri icin proxy controller'i.
 * JWT + @PreAuthorize ile korunur; panel dogrudan :15672'ye erismez.
 */
@RestController
@RequestMapping("/api/v1/admin/rabbit")
@RequiredArgsConstructor
public class RabbitAdminController implements IRabbitAdminController {

  private final IRabbitAdminService service;

  @GetMapping("/overview")
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_READ + "')")
  public RootEntityResponse<DtoRabbitOverview> getOverview() {
    return RootEntityResponse.ok(service.getOverview());
  }

  @GetMapping("/queues")
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_READ + "')")
  public RootEntityResponse<List<DtoRabbitQueue>> listQueues() {
    return RootEntityResponse.ok(service.listQueues());
  }

  @GetMapping("/queues/{name}")
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_READ + "')")
  public RootEntityResponse<DtoRabbitQueue> getQueue(@PathVariable("name") String name) {
    return RootEntityResponse.ok(service.getQueue(name));
  }

  @GetMapping("/queues/{name}/messages")
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_READ + "')")
  public RootEntityResponse<List<DtoRabbitMessage>> peekMessages(
      @PathVariable("name") String name,
      @RequestParam(value = "count", defaultValue = "10") int count) {
    return RootEntityResponse.ok(service.peekMessages(name, count));
  }

  @PostMapping("/queues/{name}/purge")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_MANAGE + "')")
  public RootEntityResponse<Void> purgeQueue(@PathVariable("name") String name) {
    service.purgeQueue(name);
    RootEntityResponse<Void> response = new RootEntityResponse<>();
    response.setResult(true);
    response.setMessage("Queue '" + name + "' purge edildi");
    return response;
  }

  @PostMapping("/queues/{name}/republish")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_MANAGE + "')")
  public RootEntityResponse<Void> republish(
      @PathVariable("name") String name,
      @Valid @RequestBody RepublishRequest request) {
    service.republishMessage(name, request.getTargetQueue(), request.getPayload(), request.getContentType());
    RootEntityResponse<Void> response = new RootEntityResponse<>();
    response.setResult(true);
    response.setMessage("Mesaj '" + request.getTargetQueue() + "' kuyruguna publish edildi");
    return response;
  }

  /**
   * Hizli hacim kontrolu: queue content'ini silmek icin DELETE (HTTP semantigine daha uygun).
   * POST /purge ile ayni isi yapar; panel UI hangisini tercih ederse secebilir.
   */
  @DeleteMapping("/queues/{name}/contents")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_MANAGE + "')")
  public RootEntityResponse<Void> deleteContents(@PathVariable("name") String name) {
    return purgeQueue(name);
  }
}
