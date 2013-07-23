/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.view;

import org.obiba.opal.web.gwt.app.client.presenter.ItemSelectorPresenter.EnterKeyHandler;
import org.obiba.opal.web.gwt.app.client.presenter.ItemSelectorPresenter.ItemInputDisplay;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class KeyValueItemInputView implements ItemInputDisplay {

  @UiTemplate("KeyValueItemInputView.ui.xml")
  interface MyUiBinder extends UiBinder<HTMLPanel, KeyValueItemInputView> {}

  private static final MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  private final HTMLPanel container;

  @UiField
  TextBox keyTextBox;

  @UiField
  TextBox valueTextBox;

  private EnterKeyHandler enterKeyHandler;

  public KeyValueItemInputView() {
    container = uiBinder.createAndBindUi(this);
    addEnterKeyHandler();
  }

  @Override
  public void clear() {
    keyTextBox.setText("");
    valueTextBox.setText("");

    keyTextBox.setFocus(true);
  }

  @Override
  public String getItem() {
    String key = keyTextBox.getText().trim();
    String value = valueTextBox.getText().trim();

    return key.length() != 0 && value.length() != 0 ? keyTextBox.getText() + " = " + valueTextBox.getText() : "";
  }

  @Override
  public Widget asWidget() {
    return container;
  }

  @Override
  public void setEnterKeyHandler(EnterKeyHandler handler) {
    enterKeyHandler = handler;
  }

  private void addEnterKeyHandler() {
    valueTextBox.addKeyDownHandler(new KeyDownHandler() {

      @Override
      public void onKeyDown(KeyDownEvent event) {
        if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          if(enterKeyHandler != null) {
            enterKeyHandler.enterKeyPressed();
          }
        }
      }
    });
  }
}