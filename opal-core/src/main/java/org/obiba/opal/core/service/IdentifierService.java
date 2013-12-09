/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.core.service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.magma.ValueTable;
import org.obiba.magma.ValueTableWriter;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.views.View;
import org.obiba.opal.core.magma.FunctionalUnitView;
import org.obiba.opal.core.magma.PrivateVariableEntityMap;
import org.obiba.opal.core.unit.FunctionalUnit;

/**
 *
 */
public interface IdentifierService {

  Variable createKeyVariable(@Nullable ValueTable privateView, @NotNull String keyVariableName);

  /**
   * Write the key variable and the identifier variables values; update the participant key private/public map.
   */
  void copyParticipantIdentifiers(VariableEntity publicEntity, ValueTable privateView,
      PrivateVariableEntityMap entityMap, ValueTableWriter keysTableWriter);

  View createPrivateView(String viewName, ValueTable participantTable, FunctionalUnit unit, @Nullable String select);

  FunctionalUnitView createPublicView(FunctionalUnitView participantTable, boolean allowIdentifierGeneration,
      boolean ignoreUnknownIdentifier);
}
