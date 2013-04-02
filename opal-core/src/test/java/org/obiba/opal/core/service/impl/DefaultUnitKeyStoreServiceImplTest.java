/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.obiba.core.service.PersistenceManager;
import org.obiba.core.util.FileUtil;
import org.obiba.opal.core.domain.unit.UnitKeyStoreState;
import org.obiba.opal.core.unit.UnitKeyStore;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link DefaultUnitKeyStoreServiceImpl}.
 */
public class DefaultUnitKeyStoreServiceImplTest {
  //
  // Instance Variables
  //

  private DefaultUnitKeyStoreServiceImpl unitKeyStoreService;

  private PersistenceManager mockPersistenceManager;

  //
  // Fixture Methods (setUp / tearDown)
  //

  @Before
  public void setUp() {
    mockPersistenceManager = createMock(PersistenceManager.class);

    unitKeyStoreService = new DefaultUnitKeyStoreServiceImpl(createPasswordCallbackHandler());
    unitKeyStoreService.setPersistenceManager(mockPersistenceManager);
  }

  //
  // Test Methods
  //

  @Test(expected = IllegalArgumentException.class)
  public void testGetUnitKeyStoreThrowsExceptionOnNullUnitName() {
    unitKeyStoreService.getUnitKeyStore(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetUnitKeyStoreThrowsExceptionOnZeroLengthUnitName() {
    unitKeyStoreService.getUnitKeyStore("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetUnitKeyStoreThrowsExceptionOnWhitespaceOnlyUnitName() {
    unitKeyStoreService.getUnitKeyStore(" \t \n \r\n");
  }

  @Test
  public void testGetUnitKeyStore() throws IOException {
    UnitKeyStoreState expectedUnitKeyStoreStateTemplate = new UnitKeyStoreState();
    expectedUnitKeyStoreStateTemplate.setUnit("my-unit");
    UnitKeyStoreState matchedUnitKeyStoreState = new UnitKeyStoreState();
    matchedUnitKeyStoreState.setUnit("my-unit");
    matchedUnitKeyStoreState.setKeyStore(getTestKeyStoreByteArray());
    expect(mockPersistenceManager.matchOne(eqUnitKeyStoreState(expectedUnitKeyStoreStateTemplate)))
        .andReturn(matchedUnitKeyStoreState);

    replay(mockPersistenceManager);

    UnitKeyStore unitKeyStore = unitKeyStoreService.getUnitKeyStore("my-unit");

    verify(mockPersistenceManager);

    UnitKeyStore expectedUnitKeyStore = new UnitKeyStore("my-unit", null);
    assertEquals(expectedUnitKeyStore.getUnitName(), unitKeyStore.getUnitName());
  }

  @Test
  public void testGetOrCreateUnitKeyStoreCreatesTheKeyStoreIfItDoesNotExist()
      throws IOException, UnsupportedCallbackException {
    UnitKeyStoreState expectedUnitKeyStoreStateTemplate = new UnitKeyStoreState();
    expectedUnitKeyStoreStateTemplate.setUnit("my-unit");
    expect(mockPersistenceManager.matchOne(eqUnitKeyStoreState(expectedUnitKeyStoreStateTemplate))).andReturn(null)
        .atLeastOnce();
    expect(mockPersistenceManager.save((UnitKeyStoreState) anyObject())).andReturn(new UnitKeyStoreState());

    replay(mockPersistenceManager);

    UnitKeyStore unitKeyStore = unitKeyStoreService.getOrCreateUnitKeyStore("my-unit");

    verify(mockPersistenceManager);

    assertNotNull(unitKeyStore);
  }

  //
  // Helper Methods
  //

  private CallbackHandler createPasswordCallbackHandler() {
    return new CallbackHandler() {
      public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for(Callback callback : callbacks) {
          if(callback instanceof PasswordCallback) {
            ((PasswordCallback) callback).setPassword("password".toCharArray());
          }
        }
      }
    };
  }

  private byte[] getTestKeyStoreByteArray() throws IOException {

    byte[] barray;
    ByteArrayOutputStream baos = null;
    InputStream testKeyStoreStream = null;
    try {
      baos = new ByteArrayOutputStream();

      testKeyStoreStream = new FileInputStream(
          FileUtil.getFileFromResource("DefaultUnitKeyStoreServiceImplTest/opal.jks"));
      while(testKeyStoreStream.available() != 0) {
        byte[] buf = new byte[1024];
        int bytesRead = testKeyStoreStream.read(buf);
        baos.write(buf, 0, bytesRead);
      }

      barray = baos.toByteArray();
    } finally {
      if(baos != null) baos.close();
      if(testKeyStoreStream != null) testKeyStoreStream.close();
    }
    return barray;

  }

  //
  // Inner Classes
  //

  static class UnitKeyStoreStateMatcher implements IArgumentMatcher {

    private UnitKeyStoreState expected;

    public UnitKeyStoreStateMatcher(UnitKeyStoreState expected) {
      this.expected = expected;
    }

    @Override
    public boolean matches(Object actual) {
      if(actual instanceof UnitKeyStoreState) {
        return ((UnitKeyStoreState) actual).getUnit().equals(expected.getUnit());
      } else {
        return false;
      }
    }

    @Override
    public void appendTo(StringBuffer buffer) {
      buffer.append("eqUnitKeyStoreState(");
      buffer.append(expected.getClass().getName());
      buffer.append(" with unit \"");
      buffer.append(expected.getUnit());
      buffer.append("\")");
    }

  }

  static UnitKeyStoreState eqUnitKeyStoreState(UnitKeyStoreState in) {
    EasyMock.reportMatcher(new UnitKeyStoreStateMatcher(in));
    return null;
  }

}
