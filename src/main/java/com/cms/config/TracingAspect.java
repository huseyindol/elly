package com.cms.config;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aspect for automatic method-level tracing.
 * Creates spans for all Service and Repository methods in Zipkin.
 */
@Aspect
@Component
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true")
public class TracingAspect {

  private final ObservationRegistry observationRegistry;

  public TracingAspect(ObservationRegistry observationRegistry) {
    this.observationRegistry = observationRegistry;
  }

  /**
   * Pointcut for all public methods in Service classes
   */
  @Pointcut("execution(public * com.cms.service.impl.*.*(..))")
  public void serviceLayer() {
  }

  /**
   * Pointcut for all public methods in Repository classes
   */
  @Pointcut("execution(public * com.cms.repository.*.*(..))")
  public void repositoryLayer() {
  }

  /**
   * Pointcut for all public methods in Controller classes
   */
  @Pointcut("execution(public * com.cms.controller.impl.*.*(..))")
  public void controllerLayer() {
  }

  /**
   * Trace all service and repository methods.
   *
   * <p><b>Exception handling:</b> {@link Observation#observe(java.util.function.Supplier)}
   * checked exception kabul etmedigi icin lambda icinde Throwable'i sarmak gerekiyor;
   * ama bunu yaparken kendi {@code RuntimeException}'larimizi ({@link com.cms.exception.BaseException}
   * dahil) sarmamaliyiz — aksi halde {@code GlobalExceptionHandler.handleBaseException}
   * eslesemez ve tum hatalar 500 {@code RUNTIME_ERROR} olarak doner.
   *
   * <p>Bu nedenle yalnizca <b>checked Throwable</b>'lar sarilir; RuntimeException ve
   * Error oldugu gibi yeniden firlatılir.
   */
  @Around("serviceLayer() || repositoryLayer()")
  public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    String spanName = className + "." + methodName;

    return Observation.createNotStarted(spanName, observationRegistry)
        .lowCardinalityKeyValue("class", className)
        .lowCardinalityKeyValue("method", methodName)
        .observe(() -> {
          try {
            return joinPoint.proceed();
          } catch (RuntimeException re) {
            // BaseException ve diger RuntimeException'lar olduklari gibi gecsin —
            // GlobalExceptionHandler dogru HTTP status'unu turetebilir.
            throw re;
          } catch (Error err) {
            throw err;
          } catch (Throwable t) {
            // Sadece checked Throwable'lar (genelde olmaz ama olabilir) sarilir.
            throw new RuntimeException(t);
          }
        });
  }
}
