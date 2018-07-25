/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.project;

import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;

import org.obiba.magma.Datasource;
import org.obiba.magma.Timestamped;
import org.obiba.magma.Timestamps;
import org.obiba.magma.Value;
import org.obiba.magma.ValueTable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.datasource.nil.NullDatasource;
import org.obiba.opal.core.domain.Project;
import org.obiba.opal.web.magma.DatasourceResource;
import org.obiba.opal.web.model.Magma;
import org.obiba.opal.web.model.Projects;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.obiba.opal.web.model.Projects.ProjectDto;

public class Dtos {

  private static final Logger log = LoggerFactory.getLogger(Dtos.class);

  private Dtos() {}

  public static ProjectDto asDto(Project project, @NotNull String directory) {
    ProjectDto.Builder builder = ProjectDto.newBuilder() //
        .setName(project.getName()) //
        .setTitle(project.getTitle()) //
        .setDirectory(directory) //
        .setLink(UriBuilder.fromPath("/").path(ProjectResource.class).build(project.getName()).toString())
        .setArchived(project.isArchived());
    if(project.hasDescription()) builder.setDescription(project.getDescription());
    if(project.hasTags()) builder.addAllTags(project.getTags());
    if(project.hasDatabase()) builder.setDatabase(project.getDatabase());
    if(project.hasVCFStoreService()) builder.setVcfStoreService(project.getVCFStoreService());
    Datasource datasource = project.getDatasource();
    Magma.DatasourceDto.Builder dsDtoBuilder;
    try {
      dsDtoBuilder = org.obiba.opal.web.magma.Dtos.asDto(datasource);
    } catch (Exception e) {
      log.error("Error when accessing project's datasource: {}", project.getName(), e);
      datasource = new NullDatasource(project.getName());
      dsDtoBuilder = org.obiba.opal.web.magma.Dtos.asDto(datasource);
    }
    builder.setDatasource(dsDtoBuilder.setLink(UriBuilder.fromPath("/").path(DatasourceResource.class).build(project.getName()).toString()));
    builder.setTimestamps(asTimestampsDto(project, datasource));

    return builder.build();
  }

  public static Project fromDto(ProjectDto projectDto) {
    return Project.Builder.create() //
        .name(projectDto.getName()) //
        .title(projectDto.getTitle()) //
        .description(projectDto.getDescription()) //
        .database(projectDto.getDatabase()) //
        .vcfStoreService(projectDto.getVcfStoreService()) //
        .archived(projectDto.getArchived()) //
        .tags(projectDto.getTagsList()) //
        .build();
  }

  public static Project fromDto(Projects.ProjectFactoryDto projectFactoryDto) {
    return Project.Builder.create() //
        .name(projectFactoryDto.getName()) //
        .title(projectFactoryDto.getTitle()) //
        .description(projectFactoryDto.getDescription()) //
        .database(projectFactoryDto.getDatabase()) //
        .vcfStoreService(projectFactoryDto.getVcfStoreService()) //
        .tags(projectFactoryDto.getTagsList()) //
        .build();
  }

  @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
  public static Projects.ProjectSummaryDto asSummaryDto(Project project) {
    Projects.ProjectSummaryDto.Builder builder = Projects.ProjectSummaryDto.newBuilder();
    builder.setName(project.getName());

    // TODO get counts from elasticsearch
    int tableCount = 0;
    int variablesCount = 0;
    Set<String> ids = Sets.newHashSet();
    for(ValueTable table : project.getDatasource().getValueTables()) {
      tableCount++;
      variablesCount = variablesCount + table.getVariableCount();
      for(VariableEntity entity : table.getVariableEntities()) {
        ids.add(entity.getType() + ":" + entity.getIdentifier());
      }
    }
    builder.setTableCount(tableCount);
    builder.setVariableCount(variablesCount);
    builder.setEntityCount(ids.size());

    builder.setTimestamps(asTimestampsDto(project, project.getDatasource()));

    return builder.build();
  }

  /**
   * Get the best representation of project's timestamps.
   *
   * @param project
   * @param datasource
   * @return
   */
  private static Magma.TimestampsDto asTimestampsDto(Project project, Datasource datasource) {
    if (datasource.getValueTables().size() == 0)
      return asTimestampsDto(project);
    else
      return asTimestampsDto(datasource);
  }

  private static Magma.TimestampsDto asTimestampsDto(Timestamped timestamped) {
    Timestamps ts = timestamped.getTimestamps();
    Magma.TimestampsDto.Builder builder = Magma.TimestampsDto.newBuilder();
    Value created = ts.getCreated();
    if(!created.isNull()) builder.setCreated(created.toString());
    Value lastUpdate = ts.getLastUpdate();
    if(!lastUpdate.isNull()) builder.setLastUpdate(lastUpdate.toString());
    return builder.build();
  }
}
