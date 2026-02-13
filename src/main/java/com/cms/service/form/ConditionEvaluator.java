package com.cms.service.form;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.cms.entity.form.ConditionRule;
import com.cms.enums.ConditionOperator;

/**
 * Evaluates conditional visibility rules for form fields.
 * Determines if a field should be visible/active based on other field values.
 */
@Component
public class ConditionEvaluator {

  /**
   * Evaluate if a field should be visible based on its condition rule.
   * 
   * @param rule    the condition rule to evaluate
   * @param payload the current form payload (user's answers)
   * @return true if the field should be visible, false otherwise
   */
  public boolean shouldBeVisible(ConditionRule rule, Map<String, Object> payload) {
    // If no condition, always visible
    if (rule == null) {
      return true;
    }

    String dependentField = rule.getField();
    Object targetValue = rule.getValue();
    ConditionOperator operator = rule.getOperator();

    // If any required field is missing, consider the condition not met
    if (dependentField == null || operator == null) {
      return true; // Default to visible if condition is incomplete
    }

    Object actualValue = payload.get(dependentField);

    // If the dependent field has no value, condition is not met
    if (actualValue == null) {
      return false;
    }

    return evaluateCondition(actualValue, operator, targetValue);
  }

  private boolean evaluateCondition(Object actualValue, ConditionOperator operator, Object targetValue) {
    switch (operator) {
      case EQUALS:
        return isEqual(actualValue, targetValue);
      case NOT_EQUALS:
        return !isEqual(actualValue, targetValue);
      case GT:
        return compareNumeric(actualValue, targetValue) > 0;
      case LT:
        return compareNumeric(actualValue, targetValue) < 0;
      default:
        return true;
    }
  }

  private boolean isEqual(Object actual, Object target) {
    if (actual == null && target == null) {
      return true;
    }
    if (actual == null || target == null) {
      return false;
    }

    // Handle numeric comparisons
    if (actual instanceof Number && target instanceof Number) {
      return ((Number) actual).doubleValue() == ((Number) target).doubleValue();
    }

    // Handle string comparisons
    return actual.toString().equals(target.toString());
  }

  private int compareNumeric(Object actual, Object target) {
    try {
      double actualNum = toDouble(actual);
      double targetNum = toDouble(target);
      return Double.compare(actualNum, targetNum);
    } catch (NumberFormatException e) {
      // If not numeric, fall back to string comparison
      return actual.toString().compareTo(target.toString());
    }
  }

  private double toDouble(Object value) {
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return Double.parseDouble(value.toString());
  }
}
