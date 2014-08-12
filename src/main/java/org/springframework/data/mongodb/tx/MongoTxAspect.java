package org.springframework.data.mongodb.tx;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;


@Aspect
@Component
public class MongoTxAspect {
  @Around("@annotation(org.springframework.data.mongodb.tx.MongoTx)")
  public Object executeWithinMongoTx(final ProceedingJoinPoint pjp) throws Throwable {
    if (MongoTxConfigHolder.get() != null) {
      //someone earlier in the call chain sets a mongo tx config so leave it as is and proceed
      return pjp.proceed();
    } else {
      try {
        //we are the outermost around aspect so configure  mongo tx
        final MongoTx mongoTx = getMongoTxAnnotation(pjp);
        MongoTxConfigHolder.registerConfig(configFromAnnotation(mongoTx));
        return pjp.proceed();
      } finally {
        MongoTxConfigHolder.resetConfig();
      }
    }
  }

  private MongoTx getMongoTxAnnotation(ProceedingJoinPoint pjp) {
    final Class<?> targetClass = AopProxyUtils.ultimateTargetClass(pjp.getTarget());
    final MethodSignature signature = (MethodSignature) pjp.getSignature();
    final Method mostSpecificMethod = AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass);
    final MongoTx mongoTxAnnotation = mostSpecificMethod.getAnnotation(MongoTx.class);
    if (mongoTxAnnotation == null) {
      throw new IllegalStateException(
        String.format("cannot determine MongoTx annotation in %s with found target class %s and method %s.",
          pjp,
          targetClass,
          mostSpecificMethod));
    }
    return mongoTxAnnotation;
  }

  private MongoTxConfig configFromAnnotation(final MongoTx mongoTx) {
    ReadPreference readPreference = null;
    if (StringUtils.isNotBlank(mongoTx.readPreference())) {
      readPreference = ReadPreference.valueOf(mongoTx.readPreference());
    }

    WriteConcern writeConcern = null;
    final String writeConcernStr = mongoTx.writeConcern();
    if (StringUtils.isNotBlank(writeConcernStr) && !MongoTx.DEFAULT_WRITE_CONCERN.equals(writeConcernStr)) {
      writeConcern = WriteConcern.valueOf(writeConcernStr);
      if (writeConcern == null) {
        throw new IllegalArgumentException("writeConcern '" + writeConcernStr + "' not known");
      }
    }
    return new MongoTxConfig(writeConcern, readPreference);
  }
}
