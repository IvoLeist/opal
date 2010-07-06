/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.widgets.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 *
 */
public class FileSelectionEvent extends GwtEvent<FileSelectionEvent.Handler> {
  //
  // Static Variables
  //

  private static Type<Handler> TYPE;

  //
  // Instance Variables
  //

  private Object source;

  private String selectedFile;

  //
  // Constructors
  //

  public FileSelectionEvent(Object source, String selectedFile) {
    this.source = source;
    this.selectedFile = selectedFile;
  }

  //
  // GwtEvent Methods
  //

  @Override
  protected void dispatch(Handler handler) {
    handler.onFileSelection(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  //
  // Methods
  //

  public static Type<Handler> getType() {
    return TYPE != null ? TYPE : (TYPE = new Type<Handler>());
  }

  @Override
  public Object getSource() {
    return source;
  }

  public String getSelectedFile() {
    return selectedFile;
  }

  //
  // Inner Classes / Interfaces
  //

  public interface Handler extends EventHandler {

    public void onFileSelection(FileSelectionEvent event);
  }
}
