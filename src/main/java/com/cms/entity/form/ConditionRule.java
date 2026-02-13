package com.cms.entity.form;

import java.io.Serializable;

import com.cms.enums.ConditionOperator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Condition rule for conditional field visibility.
 * A field with this condition will only be visible/active if the condition
 * evaluates to true.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConditionRule implements Serializable {
  private String field; // The field ID to check
  private ConditionOperator operator; // EQUALS, NOT_EQUALS, GT, LT
  private Object value; // The value to compare against
}
