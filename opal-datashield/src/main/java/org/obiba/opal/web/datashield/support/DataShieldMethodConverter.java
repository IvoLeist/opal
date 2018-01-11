/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.datashield.support;

import org.obiba.opal.datashield.DataShieldMethod;
import org.obiba.opal.web.model.DataShield.DataShieldMethodDto;

/**
 *
 */
public interface DataShieldMethodConverter {

  /**
   * @param dto
   * @return
   */
  public boolean canParse(DataShieldMethodDto dto);

  /**
   * @param dto
   * @return
   */
  public DataShieldMethod parse(DataShieldMethodDto dto);

  /**
   * @param method
   * @return
   */
  public boolean accept(DataShieldMethod method);

  /**
   * @param method
   * @return
   */
  public DataShieldMethodDto asDto(DataShieldMethod method);

}
