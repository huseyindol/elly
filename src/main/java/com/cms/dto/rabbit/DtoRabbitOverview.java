package com.cms.dto.rabbit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Broker seviyesi ozet — panel landing card icin.
 * Management API /api/overview response'undan map edilir.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoRabbitOverview {

  private String rabbitmqVersion;
  private String erlangVersion;
  private String clusterName;

  private Long totalMessages;
  private Long totalConsumers;
  private Integer queueCount;
  private Integer exchangeCount;
  private Integer connectionCount;
  private Integer channelCount;
}
