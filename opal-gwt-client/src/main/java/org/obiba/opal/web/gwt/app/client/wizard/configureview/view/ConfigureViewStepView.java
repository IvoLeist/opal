/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.wizard.configureview.view;

import org.obiba.opal.web.gwt.app.client.wizard.configureview.presenter.ConfigureViewStepPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class ConfigureViewStepView extends Composite implements ConfigureViewStepPresenter.Display {
  //
  // Static Variables
  //

  private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);

  //
  // Instance Variables
  //
  @UiField
  DeckPanel helpPanelDecks;

  @UiField
  SimplePanel entitiesTabPanel;

  @UiField
  SimplePanel dataTabPanel;

  @UiField
  TabLayoutPanel viewTabs;

  //
  // Constructors
  //

  public ConfigureViewStepView() {
    initWidget(uiBinder.createAndBindUi(this));
  }

  //
  // ConfigureViewStepPresenter.Display Methods
  //

  public Widget asWidget() {
    return this;
  }

  public void startProcessing() {
  }

  public void stopProcessing() {
  }

  //
  // Inner Classes / Interfaces
  //

  @UiTemplate("ConfigureViewStepView.ui.xml")
  interface ViewUiBinder extends UiBinder<Widget, ConfigureViewStepView> {
  }

  @Override
  public DeckPanel getHelpDeck() {
    return helpPanelDecks;
  }

  @Override
  public void addEntitiesTabWidget(Widget widget) {
    entitiesTabPanel.clear();
    entitiesTabPanel.add(widget);
  }

  @Override
  public TabLayoutPanel getViewTabs() {
    return viewTabs;
  }

  @Override
  public void addDataTabWidget(Widget widget) {
    dataTabPanel.clear();
    dataTabPanel.add(widget);
  }

  @Override
  public void displayTab(int tabNumber) {
    viewTabs.selectTab(tabNumber);
  }
}
