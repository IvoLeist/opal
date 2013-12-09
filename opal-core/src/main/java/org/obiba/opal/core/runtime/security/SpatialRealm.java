/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.runtime.security;

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.obiba.opal.core.runtime.security.support.SubjectPermissionsConverterRegistry;
import org.obiba.opal.core.service.security.SubjectAclService;
import org.obiba.opal.core.service.security.SubjectAclService.Subject;
import org.obiba.opal.core.service.security.SubjectAclService.SubjectAclChangeCallback;
import org.obiba.opal.core.service.security.SubjectAclService.SubjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import eu.flatwhite.shiro.spatial.SingleSpaceRelationProvider;
import eu.flatwhite.shiro.spatial.SingleSpaceResolver;
import eu.flatwhite.shiro.spatial.Spatial;
import eu.flatwhite.shiro.spatial.SpatialPermissionResolver;
import eu.flatwhite.shiro.spatial.finite.Node;
import eu.flatwhite.shiro.spatial.finite.NodeRelationProvider;
import eu.flatwhite.shiro.spatial.finite.NodeResolver;
import eu.flatwhite.shiro.spatial.finite.NodeSpace;

@Component
public class SpatialRealm extends AuthorizingRealm implements RolePermissionResolver {

  private final SubjectAclService subjectAclService;

  private final RolePermissionResolver rolePermissionResolver;

  private final SubjectPermissionsConverterRegistry subjectPermissionsConverterRegistry;

  private Cache<Subject, Collection<Permission>> rolePermissionCache;

  @Autowired
  public SpatialRealm(SubjectAclService subjectAclService,
      SubjectPermissionsConverterRegistry subjectPermissionsConverterRegistry) {
    if(subjectAclService == null) throw new IllegalArgumentException("subjectAclService cannot be null");
    this.subjectAclService = subjectAclService;
    this.subjectPermissionsConverterRegistry = subjectPermissionsConverterRegistry;

    setPermissionResolver(new SpatialPermissionResolver(new SingleSpaceResolver(new RestSpace()), new NodeResolver(),
        new SingleSpaceRelationProvider(new NodeRelationProvider())));
    rolePermissionResolver = new GroupPermissionResolver();
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    // This realm is not used for authentication
    return false;
  }

  @PostConstruct
  public void registerListener() {
    if(isAuthorizationCachingEnabled()) {
      subjectAclService.addListener(new SubjectAclChangeCallback() {

        @Override
        public void onSubjectAclChanged(Subject subject) {
          getAuthorizationCache().remove(subject);
          getRolePermissionCache().remove(subject);
        }
      });
    }
  }

  @Override
  public Collection<Permission> resolvePermissionsInRole(String roleString) {
    return getRolePermissionResolver().resolvePermissionsInRole(roleString);
  }

  /**
   * Overridden because the OpalSecurityManager sets {@code this} as the {@code RolePermissionResolver} on all configured
   * realms. This results the following object graph:
   * <p/>
   * <pre>
   * AuthorizingReam.rolePermissionResolver -> SpatialRealm (this)
   *      ^
   *      |
   * SpatialRealm.rolePermissionResolver -> GroupPermissionResolver
   *
   * <pre>
   * By overriding this method, we prevent an infinite loop from occurring when
   * {@code getRolePermissionResolver().resolvePermissionsInRole()} is called.
   */
  @Override
  public RolePermissionResolver getRolePermissionResolver() {
    return rolePermissionResolver;
  }

  protected Cache<Subject, Collection<Permission>> getRolePermissionCache() {
    return rolePermissionCache;
  }

  @Override
  protected void afterCacheManagerSet() {
    super.afterCacheManagerSet();
    if(isAuthorizationCachingEnabled()) {
      CacheManager cacheManager = getCacheManager();
      rolePermissionCache = cacheManager.getCache(getAuthorizationCacheName() + "_role");
    }
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    Iterable<String> perms = loadSubjectPermissions(principals);
    if(perms != null) {
      SimpleAuthorizationInfo sai = new SimpleAuthorizationInfo();
      sai.setStringPermissions(ImmutableSet.copyOf(perms));
      return sai;
    }
    return null;
  }

  @Override
  protected Object getAuthorizationCacheKey(PrincipalCollection principals) {
    return getSubject(principals);
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    return null;
  }

  private Iterable<String> loadSubjectPermissions(SubjectAclService.Subject subject) {
    return subjectPermissionsConverterRegistry.convert(subjectAclService.getSubjectPermissions(subject));
  }

  private Iterable<String> loadSubjectPermissions(PrincipalCollection principals) {
    return loadSubjectPermissions(getSubject(principals));
  }

  private SubjectAclService.Subject getSubject(PrincipalCollection principals) {
    return SubjectType.USER.subjectFor(principals.getPrimaryPrincipal().toString());
  }

  private final class GroupPermissionResolver implements RolePermissionResolver {

    @Override
    public Collection<Permission> resolvePermissionsInRole(String roleString) {
      Subject group = SubjectType.GROUP.subjectFor(roleString);
      if(isAuthorizationCachingEnabled() && getRolePermissionCache() != null) {
        Collection<Permission> cached = getRolePermissionCache().get(group);
        if(cached != null) {
          return cached;
        }
        cached = doGetGroupPermissions(group);
        getRolePermissionCache().put(group, cached);
        return cached;
      } else {
        return doGetGroupPermissions(group);
      }
    }

    private Collection<Permission> doGetGroupPermissions(Subject group) {
      return ImmutableList
          .copyOf(Iterables.transform(loadSubjectPermissions(group), new Function<String, Permission>() {

            @Override
            public Permission apply(String from) {
              return getPermissionResolver().resolvePermission(from);
            }
          }));
    }

  }

  /**
   * Overriden to make plural form resources part of non-plural form resources.
   * <p/>
   * That is, this space considers plural form sub-resources as related to non-plural form sub-resources:
   * <p/>
   * <pre>
   * /parent/kids
   * /parent/kid/1
   * </pre>
   */
  static class RestSpace extends NodeSpace {
    @Override
    protected double calculateDistance(Spatial s1, Spatial s2) {

      Double d = super.calculateDistance(s1, s2);
      if(Double.isNaN(d)) {
        // Check for plural form relation
        Node n1 = (Node) s1;

        Node n2 = (Node) s2;

        int nodes = Math.min(n1.getPath().size(), n2.getPath().size());
        for(int i = 0; i < nodes; i++) {
          Node lhs = n1.getPath().get(i);
          Node rhs = n2.getPath().get(i);
          if(!lhs.getPathElem().equals(rhs.getPathElem())) {
            // Check for plural form
            return isPluralForm(lhs, rhs) ? 1 : d;
          }
        }

      }
      return d;

    }

    /**
     * Returns true when lhs is the plural form of rhs (or vice-versa), that is their node element text is identical
     * except for an additional 's' in the other node.
     *
     * @param lhs
     * @param rhs
     */
    private boolean isPluralForm(Node lhs, Node rhs) {
      String a = lhs.getPathElem();
      String b = rhs.getPathElem();
      return a.equals(b + 's') || b.equals(a + 's');
    }
  }
}
