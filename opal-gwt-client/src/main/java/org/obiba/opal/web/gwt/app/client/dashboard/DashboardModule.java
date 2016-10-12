/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.gwt.app.client.dashboard;

import org.obiba.opal.web.gwt.app.client.dashboard.presenter.DashboardPresenter;
import org.obiba.opal.web.gwt.app.client.dashboard.view.DashboardView;

import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;

public class DashboardModule extends AbstractPresenterModule {

  @Override
  protected void configure() {
    bindPresenter(DashboardPresenter.class, DashboardPresenter.Display.class, DashboardView.class,
        DashboardPresenter.Proxy.class);
  }

}
