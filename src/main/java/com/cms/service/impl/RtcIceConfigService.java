package com.cms.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cms.dto.DtoIceServer;

@Service
public class RtcIceConfigService {

  @Value("${app.rtc.stun-urls:stun:stun.l.google.com:19302}")
  private String stunUrls;

  @Value("${app.rtc.turn-urls:${TURN_URL:}}")
  private String turnUrls;

  @Value("${app.rtc.turn-username:${TURN_USERNAME:}}")
  private String turnUsername;

  @Value("${app.rtc.turn-credential:${TURN_CREDENTIAL:}}")
  private String turnCredential;

  public List<DtoIceServer> getIceServers() {
    List<DtoIceServer> servers = new ArrayList<>();

    List<String> stuns = parseUrls(stunUrls);
    if (stuns.isEmpty()) {
      stuns = List.of("stun:stun.l.google.com:19302");
    }
    servers.add(new DtoIceServer(stuns, null, null));

    List<String> turns = parseUrls(turnUrls);
    if (!turns.isEmpty()) {
      String user = (turnUsername != null && !turnUsername.isBlank()) ? turnUsername.trim() : null;
      String cred = (turnCredential != null && !turnCredential.isBlank()) ? turnCredential.trim() : null;
      servers.add(new DtoIceServer(turns, user, cred));
    }

    return servers;
  }

  private List<String> parseUrls(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }
}
