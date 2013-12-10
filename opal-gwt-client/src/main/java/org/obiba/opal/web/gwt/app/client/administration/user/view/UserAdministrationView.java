/*
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.gwt.app.client.administration.user.view;

import org.obiba.opal.web.gwt.app.client.administration.user.presenter.UserAdministrationUiHandlers;
import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.ui.celltable.ActionsColumn;
import org.obiba.opal.web.gwt.app.client.ui.celltable.ActionsProvider;
import org.obiba.opal.web.gwt.app.client.ui.celltable.HasActionHandler;
import org.obiba.opal.web.gwt.app.client.ui.celltable.IconCell;
import org.obiba.opal.web.model.client.opal.GroupDto;
import org.obiba.opal.web.model.client.opal.SubjectCredentialsDto;

import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.SimplePager;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

import static org.obiba.opal.web.gwt.app.client.administration.user.presenter.UserAdministrationPresenter.Display;
import static org.obiba.opal.web.gwt.app.client.ui.celltable.ActionsColumn.DELETE_ACTION;
import static org.obiba.opal.web.gwt.app.client.ui.celltable.ActionsColumn.EDIT_ACTION;

public class UserAdministrationView extends ViewWithUiHandlers<UserAdministrationUiHandlers> implements Display {

  interface Binder extends UiBinder<Widget, UserAdministrationView> {}

  @UiField
  SimplePager indexTablePager;

  @UiField
  CellTable<SubjectCredentialsDto> usersTable;

  @UiField
  CellTable<GroupDto> groupsTable;

  @UiField
  HasWidgets breadcrumbs;

  private final ListDataProvider<SubjectCredentialsDto> userDataProvider
      = new ListDataProvider<SubjectCredentialsDto>();

  private final ListDataProvider<GroupDto> groupDataProvider = new ListDataProvider<GroupDto>();

  @Inject
  public UserAdministrationView(Binder uiBinder, Translations translations) {
    initWidget(uiBinder.createAndBindUi(this));
    configUserTable(translations);
    configGroupTable(translations);
  }

  private void configUserTable(Translations translations) {
    indexTablePager.setDisplay(usersTable);
    usersTable.addColumn(UserColumns.NAME, translations.userNameLabel());
    usersTable.addColumn(UserColumns.GROUPS, translations.userGroupsLabel());
    usersTable.addColumn(UserColumns.STATUS, translations.userStatusLabel());
    usersTable.addColumn(UserColumns.ACTIONS, translations.actionsLabel());
    usersTable.setEmptyTableWidget(new Label(translations.noDataAvailableLabel()));
    userDataProvider.addDataDisplay(usersTable);
  }

  private void configGroupTable(Translations translations) {
    groupsTable.addColumn(GroupColumns.NAME, translations.groupNameLabel());
    groupsTable.addColumn(GroupColumns.USERS, translations.groupUsersLabel());
    groupsTable.addColumn(GroupColumns.ACTIONS, translations.actionsLabel());
    groupDataProvider.addDataDisplay(groupsTable);
  }

  @UiHandler("addUser")
  public void onAddUser(ClickEvent event) {
    getUiHandlers().onAddUser();
  }

  @Override
  public HasData<SubjectCredentialsDto> getUsersTable() {
    return usersTable;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void clear() {
    renderUserRows((JsArray<SubjectCredentialsDto>) JavaScriptObject.createArray());
    renderGroupRows((JsArray<GroupDto>) JavaScriptObject.createArray());
  }

  @Override
  public void renderUserRows(JsArray<SubjectCredentialsDto> rows) {
    userDataProvider.setList(JsArrays.toList(JsArrays.toSafeArray(rows)));
    indexTablePager.firstPage();
    userDataProvider.refresh();
    indexTablePager.setVisible(userDataProvider.getList().size() > indexTablePager.getPageSize());
  }

  @Override
  public void renderGroupRows(JsArray<GroupDto> rows) {
    groupDataProvider.setList(JsArrays.toList(JsArrays.toSafeArray(rows)));
    indexTablePager.firstPage();
    groupDataProvider.refresh();
    indexTablePager.setVisible(groupDataProvider.getList().size() > indexTablePager.getPageSize());
  }

  @Override
  public HasWidgets getBreadcrumbs() {
    return breadcrumbs;
  }

  @Override
  public HasActionHandler<SubjectCredentialsDto> getUsersActions() {
    return UserColumns.ACTIONS;
  }

  @Override
  public HasActionHandler<GroupDto> getGroupsActions() {
    return GroupColumns.ACTIONS;
  }

  private static final class UserColumns {

    static final Column<SubjectCredentialsDto, String> NAME = new TextColumn<SubjectCredentialsDto>() {

      @Override
      public String getValue(SubjectCredentialsDto object) {
        return object.getName();
      }
    };

    static final Column<SubjectCredentialsDto, String> GROUPS = new TextColumn<SubjectCredentialsDto>() {

      @Override
      public String getValue(SubjectCredentialsDto object) {
        return object.getGroupsCount() > 0 ? object.getGroupsArray().join(", ") : "";
      }
    };

    static final Column<SubjectCredentialsDto, Boolean> STATUS = new Column<SubjectCredentialsDto, Boolean>(
        new IconCell<Boolean>() {
          @Override
          public IconType getIconType(Boolean value) {
            return value ? IconType.OK : IconType.REMOVE;
          }
        }) {
      @Override
      public Boolean getValue(SubjectCredentialsDto object) {
        return object.getEnabled();
      }
    };

    static final ActionsColumn<SubjectCredentialsDto> ACTIONS = new ActionsColumn<SubjectCredentialsDto>(
        new ActionsProvider<SubjectCredentialsDto>() {

          @Override
          public String[] allActions() {
            return new String[] { EDIT_ACTION, DELETE_ACTION, ENABLE_ACTION, DISABLE_ACTION };
          }

          @Override
          public String[] getActions(SubjectCredentialsDto value) {
            if(value.getEnabled()) {
              return new String[] { EDIT_ACTION, DELETE_ACTION, DISABLE_ACTION };
            }
            return new String[] { EDIT_ACTION, DELETE_ACTION, ENABLE_ACTION };
          }
        });

  }

  private static final class GroupColumns {

    static final Column<GroupDto, String> NAME = new TextColumn<GroupDto>() {

      @Override
      public String getValue(GroupDto object) {
        return object.getName();
      }
    };

    static final Column<GroupDto, String> USERS = new TextColumn<GroupDto>() {

      @Override
      public String getValue(GroupDto object) {
        return object.getUsersCount() > 0 ? object.getUsersArray().join(", ") : "";
      }
    };

    static final ActionsColumn<GroupDto> ACTIONS = new ActionsColumn<GroupDto>(new ActionsProvider<GroupDto>() {

      @Override
      public String[] allActions() {
        return new String[] { DELETE_ACTION };
      }

      @Override
      public String[] getActions(GroupDto value) {
        return allActions();
      }
    });
  }
}
