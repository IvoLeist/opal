package org.obiba.opal.web.gwt.app.client.permissions.view;

import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.permissions.presenter.AddResourcePermissionModalPresenter;
import org.obiba.opal.web.gwt.app.client.permissions.presenter.ResourcePermissionModalUiHandlers;
import org.obiba.opal.web.gwt.app.client.permissions.support.ResourcePermissionType;
import org.obiba.opal.web.gwt.app.client.ui.Chooser;
import org.obiba.opal.web.gwt.app.client.ui.Modal;
import org.obiba.opal.web.model.client.opal.Subject;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

public class AddResourcePermissionModalView
    extends AbstractResourcePermissionModalView<ResourcePermissionModalUiHandlers>
    implements AddResourcePermissionModalPresenter.Display {

  interface Binder extends UiBinder<Widget, AddResourcePermissionModalView> {}

  private final Translations translations;

  @UiField
  TextBox principal;

  @UiField
  ControlGroup principalGroup;

  @UiField
  ControlGroup permissionsGroup;

  @Inject
  public AddResourcePermissionModalView(Binder uiBinder, EventBus eventBus, Translations translations) {
    super(eventBus);
    initWidget(uiBinder.createAndBindUi(this));
    this.translations = translations;
  }

  @Override
  public void setData(ResourcePermissionType type, Subject.SubjectType subjectType) {
    createPermissionRadios(type, null);
    dialog.setTitle(translations.addSubjectPermissionMap().get(subjectType.getName() + ".title"));
  }

  @Override
  public String getPermission() {
    return getSelectedPermission();
  }

  @Override
  public HasText getPrincipal() {
    return principal;
  }

  @Override
  public void close() {
    dialog.hide();
  }

  @Override
  public void showError(String message, FormField field) {
    ControlGroup group = null;
    if(field != null) {
      switch(field) {
        case PRINCIPAL:
          group = principalGroup;
          break;
        case PERMISSIONS:
          group = permissionsGroup;
          break;
      }
    }
    if(group == null) {
      dialog.addAlert(message, AlertType.ERROR);
    } else {
      dialog.addAlert(message, AlertType.ERROR, group);
    }
  }

  @Override
  public void clearErrors() {
    dialog.closeAlerts();
  }

  @UiHandler("saveButton")
  public void onSaveButton(ClickEvent event) {
    getUiHandlers().save();
  }

  @UiHandler("cancelButton")
  public void onCloseButton(ClickEvent event) {
    close();
  }

}
