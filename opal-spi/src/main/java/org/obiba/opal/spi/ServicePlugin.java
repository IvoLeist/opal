/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.spi;

import java.util.Properties;

/**
 * Base pluggable service interface.
 */
public interface ServicePlugin {

  /**
   * Service plugin unique name.
   */
  String getName();

  /**
   * Set the configuration of the service.
   *
   * @param properties
   */
  void configure(Properties properties);

  /**
   * Check if service is up and running.
   *
   * @return
   */
  boolean isRunning();

  /**
   * Start the service (and do the initialization work).
   */
  void start();

  /**
   * Stop the service (and do the clean-up work).
   */
  void stop();


}
