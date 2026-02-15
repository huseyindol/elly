package com.cms.entity.form;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Option definition for select, radio, and checkbox fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Option implements Serializable {
  private String label;
  private Object value;
}
