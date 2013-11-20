/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.server.httpd;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.annotation.Nullable;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.obiba.opal.core.runtime.OpalRuntime;
import org.obiba.opal.core.service.SubjectAclService;
import org.obiba.opal.core.unit.security.X509CertificateAuthenticationToken;
import org.obiba.opal.web.security.HttpAuthorizationToken;
import org.obiba.opal.web.security.HttpCookieAuthenticationToken;
import org.obiba.opal.web.security.HttpHeaderAuthenticationToken;
import org.obiba.opal.web.security.OpalAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

  private static final String OPAL_SESSION_ID_COOKIE_NAME = "opalsid";

  private static final String OPAL_REQUEST_ID_COOKIE_NAME = "opalrid";

  private SessionsSecurityManager securityManager;

  private OpalRuntime opalRuntime;

  private SubjectAclService subjectAclService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if(ThreadContext.getSubject() != null) {
      log.warn("Previous executing subject was not properly unbound from executing thread. Unbinding now.");
      ThreadContext.unbindSubject();
    }

    try {
      authenticateAndBind(request);
      filterChain.doFilter(request, response);
    } catch(AuthenticationException e) {
      response.setStatus(HttpStatus.UNAUTHORIZED_401);
    } finally {
      unbind();
    }
  }

  private SessionsSecurityManager getSecurityManager() {
    if(securityManager == null) securityManager = getSpringBean(SessionsSecurityManager.class);
    return securityManager;
  }

  private OpalRuntime getOpalRuntime() {
    if(opalRuntime == null) opalRuntime = getSpringBean(OpalRuntime.class);
    return opalRuntime;
  }

  private SubjectAclService getSubjectAclService() {
    if(subjectAclService == null) subjectAclService = getSpringBean(SubjectAclService.class);
    return subjectAclService;
  }

  private <T> T getSpringBean(Class<T> clazz) {
    return WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext()).getBean(clazz);
  }

  /**
   * This method will try to authenticate the user using the provided sessionId or the "Authorization" header. When no
   * credentials are provided, this method does nothing. This will invoke the filter chain with an anonymous subject,
   * which allows fetching public web resources.
   *
   * @param request
   */
  private void authenticateAndBind(HttpServletRequest request) {

    Subject subject = null;

    if(hasSslCert(request)) {
      subject = authenticateBySslCert(request);
    }

    if(subject == null && hasOpalAuthHeader(request)) {
      subject = authenticateByOpalAuthHeader(request);
    }

    if(subject == null && hasAuthorizationHeader(request)) {
      subject = authenticateByAuthorizationHeader(request);
    }

    if(subject == null && hasOpalSessionCookie(request) && hasOpalRequestCookie(request)) {
      subject = authenticateByCookie(request);
    }

    if(subject != null) {
      Session session = subject.getSession();
      log.trace("Binding subject {} session {} to executing thread {}", subject.getPrincipal(), session.getId(),
          Thread.currentThread().getId());
      session.touch();
      String username = subject.getPrincipal().toString();
      ensureUserHomeExists(username);
      ensureFolderPermissions(username, "/home/" + username);
      ensureFolderPermissions(username, "/tmp");
    }
  }

  private void ensureUserHomeExists(String username) {
    try {
      FileObject home = getOpalRuntime().getFileSystem().getRoot().resolveFile("/home/" + username);
      if(!home.exists()) {
        log.info("Creating user home: /home/{}", username);
        home.createFolder();
      }
    } catch(FileSystemException e) {
      log.error("Failed creating user home.", e);
    }
  }

  private void ensureFolderPermissions(String username, String path) {
    String folderNode = "/files" + path;
    String homePerm = "FILES_SHARE";
    boolean found = false;
    for(SubjectAclService.Permissions acl : getSubjectAclService()
        .getNodePermissions("opal", folderNode, SubjectAclService.SubjectType.USER)) {
      found = findPermission(acl, homePerm);
      if(found) break;
    }
    if(!found) {
      getSubjectAclService()
          .addSubjectPermission("opal", folderNode, SubjectAclService.SubjectType.USER.subjectFor(username), homePerm);
      getSubjectAclService().addSubjectPermission("opal", folderNode.replace("/files/", "/files/meta/"),
          SubjectAclService.SubjectType.USER.subjectFor(username), "FILES_META");
    }
  }

  private boolean findPermission(SubjectAclService.Permissions acl, String permission) {
    boolean found = false;
    for(String perm : acl.getPermissions()) {
      if(perm.equals(permission)) {
        found = true;
        break;
      }
    }
    return found;
  }

  @Nullable
  private Subject authenticateBySslCert(HttpServletRequest request) {
    X509Certificate[] chain = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
    if(chain == null || chain.length == 0) return null;
    AuthenticationToken token = new X509CertificateAuthenticationToken(chain[0]);
    String sessionId = extractSessionId(request);
    Subject subject = new Subject.Builder(getSecurityManager()).sessionId(sessionId).buildSubject();
    subject.login(token);
    log.info("Successfully authenticated subject {}", SecurityUtils.getSubject().getPrincipal());
    return subject;
  }

  private boolean hasSslCert(ServletRequest request) {
    X509Certificate[] chain = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
    return chain != null && chain.length > 0;
  }

  private Subject authenticateByOpalAuthHeader(HttpServletRequest request) {
    String opalAuthToken = getOpalAuthToken(request);
    AuthenticationToken token = new HttpHeaderAuthenticationToken(opalAuthToken);
    Subject subject = new Subject.Builder(getSecurityManager()).sessionId(opalAuthToken).buildSubject();
    subject.login(token);
    return subject;
  }

  private Subject authenticateByAuthorizationHeader(HttpServletRequest request) {
    String authorization = getAuthorizationHeader(request);
    String sessionId = extractSessionId(request);

    AuthenticationToken token = new HttpAuthorizationToken(OpalAuth.CREDENTIALS_HEADER, authorization);
    Subject subject = new Subject.Builder(getSecurityManager()).sessionId(sessionId).buildSubject();
    subject.login(token);
    return subject;
  }

  private Subject authenticateByCookie(HttpServletRequest request) {
    String sessionId = extractSessionId(request);
    String requestId = extractRequestId(request);
    String url = request.getRequestURI();
    AuthenticationToken token = new HttpCookieAuthenticationToken(sessionId, url, requestId);
    Subject subject = new Subject.Builder(getSecurityManager()).sessionId(sessionId).buildSubject();
    subject.login(token);
    return subject;
  }

  private void unbind() {
    try {
      if(log.isTraceEnabled()) {
        Subject s = ThreadContext.getSubject();
        if(s != null) {
          Session session = s.getSession(false);
          log.trace("Unbinding subject {} session {} from executing thread {}", s.getPrincipal(),
              session != null ? session.getId() : "null", Thread.currentThread().getId());
        }
      }
    } finally {
      ThreadContext.unbindSubject();
    }
  }

  private boolean hasOpalAuthHeader(HttpServletRequest request) {
    String header = getOpalAuthToken(request);
    return header != null && !header.isEmpty();
  }

  private String getOpalAuthToken(HttpServletRequest request) {
    return request.getHeader(OpalAuth.CREDENTIALS_HEADER);
  }

  private boolean hasOpalSessionCookie(HttpServletRequest request) {
    Cookie cookie = findCookie(request, OPAL_SESSION_ID_COOKIE_NAME);
    return cookie != null && cookie.getValue() != null;
  }

  private boolean hasAuthorizationHeader(HttpServletRequest request) {
    String header = getAuthorizationHeader(request);
    return header != null && !header.isEmpty();
  }

  private String getAuthorizationHeader(HttpServletRequest request) {
    return request.getHeader(HttpHeaders.AUTHORIZATION);
  }

  private String extractSessionId(HttpServletRequest request) {
    String sessionId = request.getHeader(OpalAuth.CREDENTIALS_HEADER);
    if(sessionId == null) {
      // Extract from the cookie (only used for GET or POST requests)
      Cookie cookie = findCookie(request, OPAL_SESSION_ID_COOKIE_NAME);
      if(cookie != null) {
        sessionId = cookie.getValue();
      }
    }
    return sessionId;
  }

  private boolean hasOpalRequestCookie(HttpServletRequest request) {
    Cookie cookie = findCookie(request, OPAL_REQUEST_ID_COOKIE_NAME);
    return cookie != null && cookie.getValue() != null;
  }

  @Nullable
  private String extractRequestId(HttpServletRequest request) {
    Cookie cookie = findCookie(request, OPAL_REQUEST_ID_COOKIE_NAME);
    return cookie == null ? null : cookie.getValue();
  }

  @Nullable
  private Cookie findCookie(HttpServletRequest request, String cookieName) {
    Cookie[] cookies = request.getCookies();
    if(cookies != null) {
      for(Cookie cookie : cookies) {
        if(cookie.getName().equals(cookieName)) {
          return cookie;
        }
      }
    }
    return null;
  }

}
