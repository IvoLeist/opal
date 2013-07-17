/*******************************************************************************
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.place;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class Places {

  private Places() {}

  public static final String login = "!login";

  public static final Place loginPlace = new Place(login);

  public static String login() {
    return dashboard;
  }

  public static final String dashboard = "!dashboard";

  public static final Place dashboardPlace = new Place(dashboard);


  public static String dashboard() {
    return dashboard;
  }

  public static final String projects = "!projects";
  public static final String project = "!project";

  public static final Place projectsPlace = new Place(projects);

  public static String projects() {
    return projects;
  }


  public static final String navigator = "!navigator";

  public static final Place navigatorPlace = new Place(navigator);

  public static String navigator() {
    return navigator;
  }

  public static final String units = "!units";

  public static final Place unitsPlace = new Place(units);

  public static String units() {
    return units;
  }

  public static final String files = "!files";

  public static final Place filesPlace = new Place(files);

  public static String files() {
    return files;
  }

  public static final String reportTemplates = "!reports";

  public static final Place reportTemplatesPlace = new Place(reportTemplates);

  public static String reportTemplates() {
    return reportTemplates;
  }

  public static final String jobs = "!jobs";

  public static final Place jobsPlace = new Place(jobs);

  public static String jobs() {
    return jobs;
  }

  public static final String administration = "!admin";

  public static final Place administrationPlace = new Place(administration);

  public static String administration() {
    return administration;
  }

  public static final String admin = "!adminpage";

  public static final Place adminPlace = new Place(admin);

  public static String admin() {
    return admin;
  }

  public static final String usersGroups = administration + ".users";

  public static final Place usersGroupsPlace = new Place(usersGroups);

  public static String usersGroups() {
    return usersGroups;
  }

  public static final String databases = "!admin.databases";

  public static final Place databasesPlace = new Place(databases);

  public static String databases() {
    return databases;
  }

  public static final String index = administration + ".index";

  public static final Place indexPlace = new Place(index);

  public static String index() {
    return index;
  }

  public static final String datashield = administration + ".datashield";

  public static final Place datashieldPlace = new Place(datashield);

  public static String datashield() {
    return datashield;
  }

  public static final String r = administration + ".r";

  public static final Place rPlace = new Place(r);

  public static String r() {
    return r;
  }

  public static final class Place extends com.google.gwt.place.shared.Place {

    final String place;

    private Map<String, String> params = new java.util.HashMap<String, String>();

    public Place(String name) {
      place = name;
    }

    public String getName() {
      return place;
    }

    public Place addParam(String name, String value) {
      params.put(name, value);
      return this;
    }

    public Set<String> getParameterNames() {
      if(params != null) {
        return params.keySet();
      } else {
        return Collections.emptySet();
      }
    }

    public String getParameter(String key, String defaultValue) {
      String value = null;

      if(params != null) {
        value = params.get(key);
      }

      if(value == null) {
        value = defaultValue;
      }
      return value;
    }
  }

}
