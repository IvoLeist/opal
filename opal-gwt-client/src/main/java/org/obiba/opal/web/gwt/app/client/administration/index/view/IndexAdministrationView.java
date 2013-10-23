/*
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.gwt.app.client.administration.index.view;

import java.util.List;

import org.obiba.opal.web.gwt.app.client.administration.index.presenter.IndexAdministrationPresenter;
import org.obiba.opal.web.gwt.app.client.administration.index.presenter.IndexAdministrationUiHandlers;
import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.ui.Table;
import org.obiba.opal.web.gwt.app.client.ui.celltable.ActionHandler;
import org.obiba.opal.web.gwt.app.client.ui.celltable.ActionsIndexColumn;
import org.obiba.opal.web.gwt.app.client.ui.celltable.ActionsProvider;
import org.obiba.opal.web.gwt.app.client.ui.celltable.CheckboxColumn;
import org.obiba.opal.web.gwt.app.client.ui.celltable.HasActionHandler;
import org.obiba.opal.web.gwt.app.client.ui.celltable.IndexStatusImageCell;
import org.obiba.opal.web.gwt.app.client.ui.celltable.ValueRenderer;
import org.obiba.opal.web.model.client.opal.TableIndexStatusDto;

import com.github.gwtbootstrap.client.ui.Alert;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.DropdownButton;
import com.github.gwtbootstrap.client.ui.SimplePager;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

import static org.obiba.opal.web.model.client.opal.ScheduleType.DAILY;
import static org.obiba.opal.web.model.client.opal.ScheduleType.HOURLY;
import static org.obiba.opal.web.model.client.opal.ScheduleType.MINUTES_15;
import static org.obiba.opal.web.model.client.opal.ScheduleType.MINUTES_30;
import static org.obiba.opal.web.model.client.opal.ScheduleType.MINUTES_5;
import static org.obiba.opal.web.model.client.opal.ScheduleType.NOT_SCHEDULED;
import static org.obiba.opal.web.model.client.opal.ScheduleType.WEEKLY;

public class IndexAdministrationView extends ViewWithUiHandlers<IndexAdministrationUiHandlers>
    implements IndexAdministrationPresenter.Display {

  interface Binder extends UiBinder<Widget, IndexAdministrationView> {}

  private static final Translations translations = GWT.create(Translations.class);

  @UiField
  Button startStopButton;

  @UiField
  Button configureButton;

  @UiField
  Button refreshIndicesButton;

  @UiField
  DropdownButton actionsDropdown;

  @UiField
  SimplePager indexTablePager;

  @UiField
  Alert selectAllAlert;

  @UiField
  Label selectAllStatus;

  @UiField
  Anchor selectAllAnchor;

  @UiField
  Anchor clearSelectionAnchor;

  @UiField
  Table<TableIndexStatusDto> indexTable;

  @UiField
  Panel breadcrumbs;

  private final ListDataProvider<TableIndexStatusDto> dataProvider = new ListDataProvider<TableIndexStatusDto>();

  private final CheckboxColumn<TableIndexStatusDto> checkboxColumn;

  private final ActionsIndexColumn<TableIndexStatusDto> actionsColumn;

  @Inject
  public IndexAdministrationView(Binder uiBinder) {
    initWidget(uiBinder.createAndBindUi(this));
    indexTablePager.setDisplay(indexTable);

    checkboxColumn = new CheckboxColumn<TableIndexStatusDto>(new TableIndexStatusDtoDisplay());
    actionsColumn = new ActionsIndexColumn<TableIndexStatusDto>(new ActionsProvider<TableIndexStatusDto>() {

      private final String[] all = new String[] { CLEAR_ACTION, INDEX_ACTION };

      @Override
      public String[] allActions() {
        return all;
      }

      @Override
      public String[] getActions(TableIndexStatusDto value) {
        return allActions();
      }
    });

    indexTable.addColumn(checkboxColumn, checkboxColumn.getTableListCheckColumnHeader());
    indexTable.addColumn(Columns.datasource, translations.datasourceLabel());
    indexTable.addColumn(Columns.table, translations.tableLabel());
    indexTable.addColumn(Columns.tableLastUpdate, translations.tableLastUpdateLabel());
    indexTable.addColumn(Columns.indexLastUpdate, translations.indexLastUpdateLabel());
    indexTable.addColumn(Columns.scheduleType, translations.scheduleLabel());
    indexTable.addColumn(Columns.status, translations.statusLabel());
    indexTable.addColumn(actionsColumn, translations.actionsLabel());
    indexTable.setEmptyTableWidget(new Label(translations.noDataAvailableLabel()));
    indexTable.setColumnWidth(checkboxColumn, 1, Style.Unit.PX);

    dataProvider.addDataDisplay(indexTable);

    actionsColumn.setActionHandler(new ActionHandler<TableIndexStatusDto>() {
      @Override
      public void doAction(TableIndexStatusDto object, String actionName) {
        if(actionName.trim().equalsIgnoreCase(CLEAR_ACTION)) {
          getUiHandlers().clear(object);
        } else if(actionName.trim().equalsIgnoreCase(INDEX_ACTION)) {
          getUiHandlers().indexNow(object);
        }
      }
    });
  }

  @UiHandler("startStopButton")
  public void onStartStop(ClickEvent event) {
    if(configureButton.isEnabled()) {
      getUiHandlers().start();
    } else {
      getUiHandlers().stop();
    }
  }

  @UiHandler("refreshIndicesButton")
  public void onRefresh(ClickEvent event) {
    getUiHandlers().refresh();
  }

  @UiHandler("configureButton")
  public void onConfigure(ClickEvent event) {
    getUiHandlers().configure();
  }

  @UiHandler("clearLink")
  public void onClear(ClickEvent event) {
    getUiHandlers().clear();
  }

  @UiHandler("scheduleLink")
  public void onSchedule(ClickEvent event) {
    getUiHandlers().schedule();
  }

  @Override
  public void renderRows(JsArray<TableIndexStatusDto> rows) {
    dataProvider.setList(JsArrays.toList(JsArrays.toSafeArray(rows)));
    indexTablePager.firstPage();
    dataProvider.refresh();
    indexTablePager.setVisible(dataProvider.getList().size() > indexTablePager.getPageSize());
  }

  @SuppressWarnings("unchecked")
  @Override
  public void clear() {
    renderRows((JsArray<TableIndexStatusDto>) JavaScriptObject.createArray());
    checkboxColumn.clearSelection();
    selectAllAlert.setVisible(false);
  }

  @Override
  public void setServiceStatus(Status status) {
    switch(status) {
      case Startable:
        startStopButton.setText(translations.startLabel());
        startStopButton.setEnabled(true);
        configureButton.setEnabled(true);
        refreshIndicesButton.setEnabled(false);
        actionsDropdown.setVisible(false);
        break;
      case Stoppable:
        startStopButton.setText(translations.stopLabel());
        startStopButton.setEnabled(true);
        configureButton.setEnabled(false);
        refreshIndicesButton.setEnabled(true);
        actionsDropdown.setVisible(true);
        break;
      case Pending:
        startStopButton.setEnabled(false);
        break;
    }
  }

  @Override
  public List<TableIndexStatusDto> getSelectedIndices() {
    return checkboxColumn.getSelectedItems();
  }

  @Override
  public void unselectIndex(TableIndexStatusDto object) {
    checkboxColumn.setSelected(object, false);
  }

  @Override
  public HasData<TableIndexStatusDto> getIndexTable() {
    return indexTable;
  }

  @Override
  public HasWidgets getBreadcrumbs() {
    return breadcrumbs;
  }

  private static final class Columns {

    static final Column<TableIndexStatusDto, String> datasource = new TextColumn<TableIndexStatusDto>() {

      @Override
      public String getValue(TableIndexStatusDto object) {
        return object.getDatasource();
      }
    };

    static final Column<TableIndexStatusDto, String> table = new TextColumn<TableIndexStatusDto>() {

      @Override
      public String getValue(TableIndexStatusDto object) {
        return object.getTable();
      }
    };

    static final Column<TableIndexStatusDto, String> tableLastUpdate = new TextColumn<TableIndexStatusDto>() {

      @Override
      public String getValue(TableIndexStatusDto object) {
        return ValueRenderer.DATETIME.render(object.getTableLastUpdate());
      }
    };

    static final Column<TableIndexStatusDto, String> indexLastUpdate = new TextColumn<TableIndexStatusDto>() {

      @Override
      public String getValue(TableIndexStatusDto object) {
        return object.getIndexLastUpdate().isEmpty() ? "-" : ValueRenderer.DATETIME.render(object.getIndexLastUpdate());
      }
    };

    static final Column<TableIndexStatusDto, String> scheduleType = new TextColumn<TableIndexStatusDto>() {

      @Override
      public String getValue(TableIndexStatusDto object) {
        if(object.getSchedule().getType().getName().equals(NOT_SCHEDULED.getName())) {
          return translations.manuallyLabel();
        }
        if(object.getSchedule().getType().getName().equals(MINUTES_5.getName())) {
          return translations.minutes5Label();
        }
        if(object.getSchedule().getType().getName().equals(MINUTES_15.getName())) {
          return translations.minutes15Label();
        }
        if(object.getSchedule().getType().getName().equals(MINUTES_30.getName())) {
          return translations.minutes30Label();
        }
        String minutes = object.getSchedule().getMinutes() < 10
            ? "0" + object.getSchedule().getMinutes()
            : String.valueOf(object.getSchedule().getMinutes());
        if(object.getSchedule().getType().getName().equals(HOURLY.getName())) {
          return translations.hourlyAtLabel().replace("{0}", minutes);
        }
        if(object.getSchedule().getType().getName().equals(DAILY.getName())) {
          return translations.dailyAtLabel().replace("{0}", Integer.toString(object.getSchedule().getHours()))
              .replace("{1}", minutes);
        }
        if(object.getSchedule().getType().getName().equals(WEEKLY.getName())) {
          return translations.weeklyAtLabel()
              .replace("{0}", translations.timeMap().get(object.getSchedule().getDay().getName()))
              .replace("{1}", Integer.toString(object.getSchedule().getHours())).replace("{2}", minutes);
        }

        return object.getSchedule().getType().toString();
      }
    };

    static final Column<TableIndexStatusDto, String> status = new Column<TableIndexStatusDto, String>(
        new IndexStatusImageCell()) {

      @Override
      public String getValue(TableIndexStatusDto tableIndexStatusDto) {
        return IndexStatusImageCell.getSrc(tableIndexStatusDto);
      }
    };
  }

  private class TableIndexStatusDtoDisplay implements CheckboxColumn.Display<TableIndexStatusDto> {

    @Override
    public Table<TableIndexStatusDto> getTable() {
      return indexTable;
    }

    @Override
    public Object getItemKey(TableIndexStatusDto item) {
      return item == null ? null : item.getDatasource() + "." + item.getTable();
    }

    @Override
    public Anchor getClearSelection() {
      return clearSelectionAnchor;
    }

    @Override
    public Anchor getSelectAll() {
      return selectAllAnchor;
    }

    @Override
    public HasText getSelectAllStatus() {
      return selectAllStatus;
    }

    @Override
    public ListDataProvider<TableIndexStatusDto> getDataProvider() {
      return dataProvider;
    }

    @Override
    public String getItemNamePlural() {
      return translations.indicesLabel().toLowerCase();
    }

    @Override
    public String getItemNameSingular() {
      return translations.indiceLabel().toLowerCase();
    }

    @Override
    public Alert getAlert() {
      return selectAllAlert;
    }
  }
}
