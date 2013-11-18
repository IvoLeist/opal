/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.gwt.app.client.magma.datasource.view;

import org.obiba.opal.web.gwt.app.client.fs.presenter.FileSelectionPresenter.Display;
import org.obiba.opal.web.gwt.app.client.magma.datasource.presenter.CsvOptionsDisplay;

import com.google.gwt.user.client.ui.HasText;
import com.gwtplatform.mvp.client.ViewImpl;

public abstract class AbstractCsvOptionsView extends ViewImpl implements CsvOptionsDisplay {

  @Override
  public void setCsvFileSelectorWidgetDisplay(Display display) {
    getCsvOptions().setCsvFileSelectorWidgetDisplay(display);
  }

  @Override
  public void setCsvFileSelectorVisible(boolean value) {
    getCsvOptions().setVisible(value);
  }

  @Override
  public HasText getRowText() {
    return getCsvOptions().getRowText();
  }

  @Override
  public HasText getCharsetText() {
    return getCsvOptions().getCharsetText();
  }

  @Override
  public HasText getFieldSeparator() {
    return getCsvOptions().getFieldSeparator();
  }

  @Override
  public HasText getQuote() {
    return getCsvOptions().getQuote();
  }

  @Override
  public void setDefaultCharset(String defaultCharset) {
    getCsvOptions().setDefaultCharset(defaultCharset);
  }

  @Override
  public void resetFieldSeparator() {
    getCsvOptions().resetFieldSeparator();
  }

  @Override
  public void resetQuote() {
    getCsvOptions().resetQuote();
  }

  @Override
  public void resetCommonCharset() {
    getCsvOptions().resetCommonCharset();
  }

  @Override
  public void clear() {
    getCsvOptions().clear();
  }

  //
  // Methods
  //

  protected abstract CsvOptionsView getCsvOptions();
}
