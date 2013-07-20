/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.workbench.view;

import com.github.gwtbootstrap.client.ui.NavTabs;
import com.github.gwtbootstrap.client.ui.NavWidget;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 */
public class BreadCrumbTabLayout extends AbstractTabPanel {

  private final String divider;

  public BreadCrumbTabLayout() {
    this("/");
  }

  public BreadCrumbTabLayout(String divider) {
    super(new NavTabs());
    addStyleName("breadcrumb-tabs");
    this.divider = divider;

    // remove all tabs after the one selected
    addSelectionHandler(new SelectionHandler<Integer>() {

      @Override
      public void onSelection(SelectionEvent<Integer> event) {
        final int idx = event.getSelectedItem();
        if(isAnimationEnabled()) {
          // wait for the end of the animation before removing descendants
          Timer t = new Timer() {
            @Override
            public void run() {
              if(!isAnimationRunning()) {
                removeDescendants(idx);
                cancel();
              }
            }
          };
          t.scheduleRepeating(10);
        } else {
          removeDescendants(idx);
        }
      }

      private void removeDescendants(int idx) {
        while(getWidgetCount() > idx + 1) {
          remove(getWidgetCount() - 1);
        }
      }
    });

    setAnimationEnabled(true);
  }

  @Override
  public void setAnimationEnabled(boolean enable) {
    super.setAnimationEnabled(enable);
  }

  @Override
  public boolean isAnimationEnabled() {
    return super.isAnimationEnabled();
  }

  @Override
  protected NavWidget newListItem(Widget item, int beforeIndex) {
    NavWidget li;
    if(beforeIndex > 0) {
      InlineLabel div = new InlineLabel(divider);
      div.setStyleName("divider");
      li = super.newListItem(div, beforeIndex);
      li.add(item);
    } else {
      li = super.newListItem(item, beforeIndex);
    }
    return li;
  }

  public void addAndSelect(Widget w, String text) {
    add(w, text);
    selectTab(w);
  }

}
