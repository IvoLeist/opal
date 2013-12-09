/*******************************************************************************
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.security;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.obiba.opal.core.domain.security.Group;
import org.obiba.opal.core.service.security.SubjectCredentialsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/group/{name}")
public class GroupResource {

  @PathParam("name")
  private String name;

  @Autowired
  private SubjectCredentialsService subjectCredentialsService;

  @GET
  public Response getGroup() {
    Group group = subjectCredentialsService.getGroup(name);
    return group == null
        ? Response.status(Response.Status.NOT_FOUND).build()
        : Response.ok().entity(Dtos.asDto(group)).build();
  }

  @DELETE
  public Response deleteGroup() {
    Group group = subjectCredentialsService.getGroup(name);
    subjectCredentialsService.delete(group);
    return Response.ok().build();
  }
}