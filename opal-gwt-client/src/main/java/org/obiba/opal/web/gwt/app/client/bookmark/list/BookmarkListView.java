package org.obiba.opal.web.gwt.app.client.bookmark.list;

import java.util.List;

import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.model.client.opal.BookmarkDto;

import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.SimplePager;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

public class BookmarkListView extends ViewWithUiHandlers<BookmarkListUiHandlers>
    implements BookmarkListPresenter.Display {

  interface Binder extends UiBinder<Widget, BookmarkListView> {}

  @UiField
  SimplePager pager;

  @UiField
  CellTable<BookmarkDto> table;

  private final Translations translations;

  private final ListDataProvider<BookmarkDto> dataProvider = new ListDataProvider<BookmarkDto>();

  @Inject
  public BookmarkListView(Binder uiBinder, Translations translations) {
    this.translations = translations;
    initWidget(uiBinder.createAndBindUi(this));
    initTable();
  }

  private void initTable() {
    table.setVisibleRange(0, 10);
    table.addColumn(Columns.NAME, translations.nameLabel());
    table.setEmptyTableWidget(new Label(translations.noDataAvailableLabel()));
    pager.setDisplay(table);
    dataProvider.addDataDisplay(table);
  }

  @Override
  public void renderRows(List<BookmarkDto> rows) {
    dataProvider.setList(rows);
    pager.firstPage();
    dataProvider.refresh();
    pager.setVisible(dataProvider.getList().size() > pager.getPageSize());
  }

  private static final class Columns {

    final static Column<BookmarkDto, String> NAME = new TextColumn<BookmarkDto>() {
      @Override
      public String getValue(BookmarkDto dto) {
        return dto.getName() + " (" + dto.getResource() + ")";
      }
    };

  }
}
