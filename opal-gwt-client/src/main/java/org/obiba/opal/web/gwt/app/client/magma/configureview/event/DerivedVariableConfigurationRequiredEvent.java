/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.magma.configureview.event;

import org.obiba.opal.web.model.client.magma.VariableDto;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Signals that a derived variable has been selected by the user.
 */
public class DerivedVariableConfigurationRequiredEvent
    extends GwtEvent<DerivedVariableConfigurationRequiredEvent.Handler> {
  //
  // Static Variables
  //

  private static final Type<Handler> TYPE = new Type<Handler>();

  //
  // Instance Variables
  //

  private final VariableDto variable;

  //
  // Constructors
  //

  public DerivedVariableConfigurationRequiredEvent(VariableDto variable) {
    this.variable = variable;
  }

  //
  // GwtEvent Methods
  //

  @Override
  protected void dispatch(Handler handler) {
    handler.onDerivedVariableConfigurationRequired(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return getType();
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  //
  // Methods
  //

  public VariableDto getVariable() {
    return variable;
  }

  //
  // Inner Classes / Interfaces
  //

  public interface Handler extends EventHandler {

    void onDerivedVariableConfigurationRequired(DerivedVariableConfigurationRequiredEvent event);
  }
}