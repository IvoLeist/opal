/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.system.subject;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.obiba.opal.core.domain.security.Bookmark;
import org.obiba.opal.core.domain.security.SubjectProfile;
import org.obiba.opal.core.service.SubjectProfileService;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.security.Dtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BookmarksResourceImpl implements BookmarksResource {

  private String principal;

  @Autowired
  private SubjectProfileService subjectProfileService;

  @Autowired
  private ApplicationContext applicationContext;

  @Override
  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  @Override
  public List<Opal.BookmarkDto> getBookmarks() {
    List<Opal.BookmarkDto> dtos = new ArrayList<>();
    for(Bookmark bookmark : getSubjectProfile().getBookmarks()) {
      dtos.add(Dtos.asDto(bookmark));
    }
    return dtos;
  }

  @Override
  public BookmarkResource getBookmark(String path) {
    BookmarkResource resource = applicationContext.getBean("bookmarkResource", BookmarkResource.class);
    resource.setPrincipal(principal);
    resource.setPath(path);
    return resource;
  }

  @Override
  public Response addBookmarks(List<String> resources) {
    SubjectProfile subjectProfile = getSubjectProfile();
    for(String resource : resources) {
      subjectProfile.addBookmark(resource);
    }
    //TODO save profile
    return Response.ok().build();
  }

  private SubjectProfile getSubjectProfile() {
    return subjectProfileService.getProfile(principal);
  }
}
