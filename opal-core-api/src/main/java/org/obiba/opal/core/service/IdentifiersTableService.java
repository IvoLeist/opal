/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.service;

import javax.annotation.Nonnull;

import org.obiba.magma.ValueTable;
import org.obiba.magma.ValueTableWriter;

/**
 *
 */
public interface IdentifiersTableService {

  /**
   * Get the identifiers value table.
   *
   * @return
   */
  ValueTable getValueTable();

  /**
   * Create a writer on the identifiers value table.
   *
   * @return
   */
  ValueTableWriter createValueTableWriter();

  /**
   * Check if identifiers value table exists.
   *
   * @return
   */
  boolean hasValueTable();

  /**
   * Get the table reference as specified by property <code>org.obiba.opal.keys.tableReference</code>.
   *
   * @return
   */
  @Nonnull
  String getTableReference();

  /**
   * Get the entity type as specified by property <code>org.obiba.opal.keys.entityType</code>.
   *
   * @return
   */
  @Nonnull
  String getEntityType();

  /**
   * Extract the datasource name from the table reference.
   *
   * @return
   */
  String getDatasourceName();

  /**
   * Extract the table name from the table reference.
   *
   * @return
   */
  String getTableName();

}
