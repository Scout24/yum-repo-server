package org.springframework.data.mongodb.tx;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class MongoTxAspect {
  @Around("@annotation(org.springframework.data.mongodb.tx.MongoTx)")
  public Object execitWithinMongoTx(final ProceedingJoinPoint pjp) throws Throwable {
    final MethodSignature signature = (MethodSignature) pjp.getSignature();
    final MongoTx mongoTx = signature.getMethod().getAnnotation(MongoTx.class);
    try {
      if (MongoTxConfigHolder.get() == null) {
        MongoTxConfigHolder.registerConfig(configFromAnnotation(mongoTx));
      }
      return pjp.proceed();
    } finally {
      MongoTxConfigHolder.resetConfig();
    }
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
