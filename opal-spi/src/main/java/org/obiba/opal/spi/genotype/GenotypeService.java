/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.spi.genotype;

import org.obiba.opal.spi.ServicePlugin;

import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * {@link ServicePlugin} to handle the genotype data.
 */
public interface GenotypeService extends ServicePlugin {

  /**
   * Get the registered store names.
   *
   * @return
   */
  Collection<String> getStores();

  /**
   * Check {@link GenotypeStore} existence.
   *
   * @param name
   * @return
   */
  boolean hasStore(String name);

  /**
   * Get the {@link GenotypeStore} with given name. Throws an exception if not found.
   *
   * @param name
   * @return
   */
  GenotypeStore getStore(String name) throws NoSuchElementException;

  /**
   * Create a {@link GenotypeStore} with given name. Throws an exception if already exists.
   *
   * @param name
   * @return the created store.
   */
  GenotypeStore createStore(String name);

  /**
   * Delete the {@link GenotypeStore} with given name. Ignore if no such store is found.
   *
   * @param name
   */
  void deleteStore(String name);

}
