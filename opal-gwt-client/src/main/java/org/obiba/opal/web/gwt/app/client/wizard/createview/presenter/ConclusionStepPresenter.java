/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.wizard.createview.presenter;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.obiba.opal.web.gwt.app.client.event.ViewConfigurationRequiredEvent;
import org.obiba.opal.web.gwt.app.client.widgets.presenter.ResourceRequestPresenter;
import org.obiba.opal.web.gwt.app.client.widgets.view.ResourceRequestView;
import org.obiba.opal.web.gwt.app.client.wizard.configureview.presenter.ConfigureViewStepPresenter;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilder;
import org.obiba.opal.web.gwt.rest.client.ResponseCodeCallback;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.inject.Inject;

public class ConclusionStepPresenter extends WidgetPresenter<ConclusionStepPresenter.Display> {
  //
  // Instance Variables
  //

  private ResourceRequestPresenter<? extends JavaScriptObject> resourceRequestPresenter;

  @Inject
  private ConfigureViewStepPresenter configureViewStepPresenter;

  //
  // Constructors
  //

  @Inject
  public ConclusionStepPresenter(final Display display, final EventBus eventBus) {
    super(display, eventBus);
  }

  //
  // WidgetPresenter Methods
  //

  @Override
  protected void onBind() {
    configureViewStepPresenter.bind();
    addEventHandlers();
  }

  @Override
  protected void onUnbind() {
    configureViewStepPresenter.unbind();
  }

  @Override
  public void revealDisplay() {
  }

  @Override
  public void refreshDisplay() {
  }

  @Override
  public Place getPlace() {
    return null;
  }

  @Override
  protected void onPlaceRequest(PlaceRequest request) {
  }

  //
  // Methods
  //

  public <T extends JavaScriptObject> void setResourceRequest(String resourceName, String resourceLink, ResourceRequestBuilder<T> requestBuilder) {
    resourceRequestPresenter = new ResourceRequestPresenter<T>(new ResourceRequestView(), eventBus, requestBuilder, new ConclusionResponseCodeCallback());
    resourceRequestPresenter.getDisplay().setResourceName(resourceName);
    resourceRequestPresenter.setSuccessCodes(200, 201);
    resourceRequestPresenter.setErrorCodes(400, 404, 405, 500);

    getDisplay().setResourceRequest(resourceRequestPresenter.getDisplay());
  }

  public void sendResourceRequest() {
    if(resourceRequestPresenter != null) {
      resourceRequestPresenter.sendRequest();
    }
  }

  public void showConfigureViewWidgets(boolean show) {
    getDisplay().showConfigureViewWidgets(show);
  }

  private void addEventHandlers() {
    super.registerHandler(getDisplay().addConfigureViewClickHandler(new ConfigureViewClickHandler()));
  }

  //
  // Inner Classes / Interfaces
  //

  public interface Display extends WidgetDisplay {

    void setResourceRequest(ResourceRequestPresenter.Display resourceRequestDisplay);

    void showConfigureViewWidgets(boolean show);

    HandlerRegistration addConfigureViewClickHandler(ClickHandler handler);
  }

  class ConclusionResponseCodeCallback implements ResponseCodeCallback {

    public void onResponseCode(Request request, Response response) {
      int statusCode = response.getStatusCode();
      if(statusCode == 200 || statusCode == 201) {
        getDisplay().showConfigureViewWidgets(true);
      }
    }
  }

  class ConfigureViewClickHandler implements ClickHandler {

    public void onClick(ClickEvent event) {
      // TODO: Create a ViewConfigurationRequiredEvent with the actual datasource and view names.
      eventBus.fireEvent(new ViewConfigurationRequiredEvent("theDatasource", "theView"));
    }
  }
}
