/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.gwt.app.client.magma.variable.view;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gwt.core.client.JsArrayString;
import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.magma.variable.presenter.BaseVariableAttributeModalPresenter;
import org.obiba.opal.web.gwt.app.client.magma.variable.presenter.VariableAttributeModalUiHandlers;
import org.obiba.opal.web.gwt.app.client.ui.*;
import org.obiba.opal.web.gwt.app.client.validator.ConstrainedModal;
import org.obiba.opal.web.model.client.opal.TaxonomyDto;
import org.obiba.opal.web.model.client.opal.VocabularyDto;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import static org.obiba.opal.web.gwt.app.client.magma.variable.presenter.VariableTaxonomyModalPresenter.Display;

/**
 *
 */
public class VariableTaxonomyModalView extends ModalPopupViewWithUiHandlers<VariableAttributeModalUiHandlers>
    implements Display {

  private final Translations translations;

  private List<TaxonomyDto> taxonomies;

  interface Binder extends UiBinder<Widget, VariableTaxonomyModalView> {}

  @UiField
  Modal modal;

  @UiField
  Button saveButton;

  @UiField
  Button cancelButton;

  @UiField
  TaxonomySelector taxonomySelector;

  @UiField
  Paragraph modalHelp;

  @Inject
  public VariableTaxonomyModalView(Binder uiBinder, EventBus eventBus, Translations translations) {
    super(eventBus);
    this.translations = translations;
    initWidget(uiBinder.createAndBindUi(this));
    modal.setTitle(translations.addAnnotation());
    modalHelp.setText(translations.addAnnotationHelp());
    new ConstrainedModal(modal);
  }

  @UiHandler("saveButton")
  public void onSave(ClickEvent event) {
    getUiHandlers().save(taxonomySelector.getTaxonomy(), taxonomySelector.getVocabulary(), taxonomySelector.getValues());
  }

  @UiHandler("cancelButton")
  public void onCancel(ClickEvent event) {
    getUiHandlers().cancel();
  }

  private TaxonomyDto getTaxonomy(String name) {
    for(TaxonomyDto taxo : taxonomies) {
      if(taxo.getName().equals(name)) return taxo;
    }
    return null;
  }

  private VocabularyDto getVocabulary(String taxoName, String vocName) {
    TaxonomyDto taxo = getTaxonomy(taxoName);
    if(taxo == null) return null;

    for(VocabularyDto voc : JsArrays.toIterable(taxo.getVocabulariesArray())) {
      if(voc.getName().equals(vocName)) return voc;
    }
    return null;
  }

  @Override
  public void setTaxonomies(List<TaxonomyDto> taxonomies) {
    taxonomySelector.setTaxonomies(taxonomies);
  }

  @Override
  public void setLocales(JsArrayString locales) {
    taxonomySelector.setLocales(locales);
  }

  @Override
  public void hideDialog() {
    modal.hide();
  }

  @Override
  public void setDialogMode(BaseVariableAttributeModalPresenter.Mode mode) {
    switch(mode) {
      case APPLY:
        modal.setTitle(translations.applyAttribute());
        modalHelp.setText(translations.applyAttributeHelp());
        break;
      case UPDATE_MULTIPLE:
        modal.setTitle(translations.editAttributes());
        modalHelp.setText(translations.editAttributesHelp());
        break;
      case DELETE:
        taxonomySelector.termSelectable(false);
        modal.setTitle(translations.removeAttributes());
        modalHelp.setText("");
        break;
      case UPDATE_SINGLE:
        modal.setTitle(translations.editAnnotation());
        modalHelp.setText(translations.editAnnotationHelp());
        break;
    }
  }

  @Override
  public void showError(@Nullable FormField formField, String message) {
    ControlGroup group = null;
    if(formField != null) {
      switch(formField) {
        case NAMESPACE:
          //group = namespaceGroup;
          break;
        case NAME:
          //group = nameGroup;
          break;
        case VALUE:
          //group = valuesGroup;
          break;
      }
    }
    if(group == null) {
      modal.addAlert(message, AlertType.ERROR);
    } else {
      modal.addAlert(message, AlertType.ERROR, group);
    }
  }

  @Override
  public void setNamespace(String namespace) {
    taxonomySelector.selectTaxonomy(namespace);
  }

  @Override
  public void setName(String name) {
    taxonomySelector.selectVocabulary(name);
  }

  @Override
  public void setLocalizedTexts(Map<String, String> localizedTexts, List<String> locales) {
    taxonomySelector.setLocalizedTexts(localizedTexts, locales);

  }

  @Override
  public void clearErrors() {
    modal.closeAlerts();
  }

}
