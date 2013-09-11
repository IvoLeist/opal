package org.obiba.opal.core.cfg;

import javax.annotation.Nullable;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.metadata.schema.OType;

public interface OrientDbService {

  <T> T execute(OrientDbTransactionCallback<T> action) throws OException;

  void registerEntityClass(Class<?>... classes);

  <T> Iterable<T> list(String sql, Object... params);

  @Nullable
  <T> T uniqueResult(String sql, Object... params);

  long count(Class<?> clazz);

  <T> Iterable<T> list(Class<T> clazz);

  void createUniqueIndex(Class<?> clazz, String property, OType type);
}