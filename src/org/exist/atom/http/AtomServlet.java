/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
package org.exist.atom.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.exist.EXistException;
import org.exist.atom.Atom;
import org.exist.atom.AtomModule;
import org.exist.atom.modules.AtomFeeds;
import org.exist.atom.modules.AtomProtocol;
import org.exist.atom.modules.Query;
import org.exist.http.BadRequestException;
import org.exist.http.NotFoundException;
import org.exist.http.servlets.Authenticator;
import org.exist.http.servlets.BasicAuthenticator;
import org.exist.http.webdav.WebDAV;
import org.exist.security.PermissionDeniedException;
import org.exist.security.SecurityManager;
import org.exist.security.User;
import org.exist.security.XmldbPrincipal;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.util.Configuration;
import org.exist.util.DatabaseConfigurationException;
import org.exist.validation.XmlLibraryChecker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;

/**
 * Implements a rest interface for exist collections as atom feeds
 *
 * @author Alex Milowski
 */
public class AtomServlet extends HttpServlet {
   
   public final static String DEFAULT_ENCODING = "UTF-8";
   public final static String CONF_NS = "http://www.exist-db.org/Vocabulary/AtomConfiguration/2006/1/0";
   
   protected final static Logger LOG = Logger.getLogger(AtomServlet.class);
   
   static class UserXmldbPrincipal implements XmldbPrincipal {
      int authMethod;
      User user;
      UserXmldbPrincipal(int authMethod,User user) {
         this.authMethod = authMethod;
         this.user = user;
      }
      public String getName() {
         return user.getName();
      }

      public String getPassword() {
         return authMethod==WebDAV.BASIC_AUTH ? user.getPassword() : user.getDigestPassword();
      }

      public boolean hasRole(String role) {
         return user.hasGroup(role);
      }
   }
   
   class ModuleContext implements AtomModule.Context {
      ServletConfig config;
      ModuleContext(ServletConfig config,String subpath) {
         this.config = config;
      }
      
      public String getDefaultCharset() {
         return formEncoding;
      }
      
      public String getParameter(String name) {
         return config.getInitParameter(name);
      }
      
      public String getContextPath() {
         // TODO: finish
         return null;
      }
      
      public URL getContextURL() {
         // TODO: finish
         return null;
      }
   }
   
   // What I want...
   //private Map<String,AtomModule> modules;
   private Map modules;
   private Map noAuth;
   
   private String formEncoding = null;
   private BrokerPool pool = null;
   private String defaultUsername = SecurityManager.GUEST_USER;
   private String defaultPassword = SecurityManager.GUEST_USER;
   
   private Authenticator authenticator;
   
   private User defaultUser;
   
   /* (non-Javadoc)
    * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
    */
   public void init(ServletConfig config) throws ServletException {
      super.init(config);
      
      // Configure BrokerPool
      try {
         if (BrokerPool.isConfigured()) {
            LOG.info("Database already started. Skipping configuration ...");
         } else {
            String confFile = config.getInitParameter("configuration");
            String dbHome = config.getInitParameter("basedir");
            String start = config.getInitParameter("start");
            
            if (confFile == null) {
               confFile = "conf.xml";
            }
            dbHome = (dbHome == null) ? config.getServletContext().getRealPath(".") :
               config.getServletContext().getRealPath(dbHome);
            LOG.info("AtomServlet: exist.home=" + dbHome);
            System.setProperty("exist.home", dbHome);
            
            File f = new File(dbHome + File.separator + confFile);
            LOG.info("reading configuration from " + f.getAbsolutePath());
            
            if (!f.canRead()) {
               throw new ServletException("configuration file " + confFile
                       + " not found or not readable");
            }
            Configuration configuration = new Configuration(confFile, dbHome);
            if (start != null && start.equals("true")) {
               startup(configuration);
            }
         }
         pool = BrokerPool.getInstance();
         String option = config.getInitParameter("use-default-user");
         boolean useDefaultUser = true;
         if (option != null) {
            useDefaultUser = option.trim().equals("true");
         }
         if (useDefaultUser) {
            option = config.getInitParameter("user");
            if (option != null) {
               defaultUsername = option;
            }
            option = config.getInitParameter("password");
            if (option != null) {
               defaultPassword = option;
            }
            defaultUser = getDefaultUser();
            if (defaultUser!=null) {
               LOG.info("Using default user "+defaultUsername+" for all unauthorized requests.");
            } else {
               LOG.error("Default user "+defaultUsername+" cannot be found.  A BASIC AUTH challenge will be the default.");
            }
         } else {
            LOG.info("No default user.  All requires must be authorized or will result in a BASIC AUTH challenge.");
            defaultUser = null;
         }
         authenticator = new BasicAuthenticator(pool);
      } catch (EXistException e) {
         throw new ServletException("No database instance available");
      } catch (DatabaseConfigurationException e) {
         throw new ServletException("Unable to configure database instance: " + e.getMessage(), e);
      }
      
      
      //get form and container encoding's
      formEncoding = config.getInitParameter("form-encoding");
      if (formEncoding == null) {
         formEncoding = DEFAULT_ENCODING;
      }
      String containerEncoding = config.getInitParameter("container-encoding");
      if (containerEncoding == null) {
         containerEncoding = DEFAULT_ENCODING;
      }
      
      //modules = new HashMap<String,AtomModule>();
      modules = new HashMap();
      noAuth = new HashMap();
      
      File dbHome = Configuration.getExistHome();
      File atomConf = new File(dbHome,"atom-services.xml");
      config.getServletContext().log("Checking for atom configuration in "+atomConf.getAbsolutePath());
      if (atomConf.exists()) {
         config.getServletContext().log("Loading configuration "+atomConf.getAbsolutePath());
         DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
         docFactory.setNamespaceAware(true);
         DocumentBuilder docBuilder = null;
         Document confDoc = null;
         InputStream is = null;
         try {
            is = new FileInputStream(atomConf);
            InputSource src = new InputSource(new InputStreamReader(is,formEncoding));
            URI docBaseURI = atomConf.toURI();
            src.setSystemId(docBaseURI.toString());
            docBuilder = docFactory.newDocumentBuilder();
            confDoc = docBuilder.parse(src);
            
            confDoc.getDocumentElement();
            NodeList moduleConfList = confDoc.getElementsByTagNameNS(CONF_NS,"module");
            for (int i=0; i<moduleConfList.getLength(); i++) {
               Element moduleConf = (Element)moduleConfList.item(i);
               String name = moduleConf.getAttribute("name");
               if (modules.get(name)!=null) {
                  throw new ServletException("Module '"+name+"' is configured more than once ( child # "+(i+1));
               }
               if ("false".equals(moduleConf.getAttribute("authenticate"))) {
                  noAuth.put(name,Boolean.TRUE);
               }
               String className = moduleConf.getAttribute("class");
               if (className!=null && className.length()>0) {
                  try {
                     Class moduleClass = Class.forName(className);
                     AtomModule amodule = (AtomModule)moduleClass.newInstance();
                     modules.put(name,amodule);
                     amodule.init(new ModuleContext(config,name));
                  } catch (Exception ex) {
                     throw new ServletException("Cannot instantiate class "+className+" for module '"+name+"' due to exception: "+ex.getMessage(),ex);
                  }
               } else {
                  Query query = new Query();
                  modules.put(name,query);
                  
                  String allowQueryPost = moduleConf.getAttribute("query-by-post");
                  if ("true".equals(allowQueryPost)) {
                     query.setQueryByPost(true);
                  }
                  
                  NodeList methodList = moduleConf.getElementsByTagNameNS(CONF_NS,"method");
                  for (int m=0; m<methodList.getLength(); m++) {
                     Element methodConf = (Element)methodList.item(m);
                     String type = methodConf.getAttribute("type");
                     if (type==null) {
                        LOG.warn("No type specified for method in module "+name);
                        continue;
                     }
                     // What I want but can't have because of JDK 1.4
                     //URI baseURI = URI.create(methodConf.getBaseURI());
                     URI baseURI = docBaseURI;
                     String queryRef = methodConf.getAttribute("query");
                     if (queryRef==null) {
                        LOG.warn("No query specified for method "+type+" in module "+name);
                        continue;
                     }
                     boolean fromClasspath = "true".equals(methodConf.getAttribute("from-classpath"));
                     Query.MethodConfiguration mconf = query.getMethodConfiguration(type);
                     if (mconf==null) {
                        LOG.warn("Unknown method "+type+" in module "+name);
                        continue;
                     }
                     String responseContentType = methodConf.getAttribute("content-type");
                     if (responseContentType!=null) {
                        mconf.setContentType(responseContentType);
                     }
                     
                     URL queryURI = null;
                     if (fromClasspath) {
                        LOG.debug("Nope. Attempting to get resource "+queryRef+" from "+Atom.class.getName());
                        queryURI = Atom.class.getResource(queryRef);
                     } else {
                        queryURI = baseURI.resolve(queryRef).toURL();
                     }
                     LOG.debug("Loading from module "+name+" method "+type+" from resource "+queryURI+" via classpath("+fromClasspath+") and ref ("+queryRef+")");
                     if (queryURI==null) {
                        throw new ServletException("Cannot find resource "+queryRef+" for module "+name);
                     }
                     mconf.setQuerySource(queryURI);
                  }
                  query.init(new ModuleContext(config,name));
                  
               }
            }
            
         } catch (IOException e) {
            LOG.warn(e);
            throw new ServletException(e.getMessage());
         } catch (SAXException e) {
            LOG.warn(e);
            throw new ServletException(e.getMessage());
         } catch (ParserConfigurationException e) {
             LOG.warn(e);
             throw new ServletException(e.getMessage());
         } catch (EXistException e) {
             LOG.warn(e);
             throw new ServletException(e.getMessage());
         } finally {
            if (is!=null) {
               try {
                  is.close();
               } catch (IOException ex) {
               }
            }
         }
      } else {
         try {
            AtomProtocol protocol = new AtomProtocol();
            modules.put("edit",protocol);
            protocol.init(new ModuleContext(config,"edit"));
            AtomFeeds feeds = new AtomFeeds();
            modules.put("content",feeds);
            feeds.init(new ModuleContext(config,"content"));
            Query query = new Query();
            query.setQueryByPost(true);
            modules.put("query",query);
            query.init(new ModuleContext(config,"query"));
            Query topics = new Query();
            modules.put("topic",topics);
            topics.getMethodConfiguration("GET").setQuerySource(topics.getClass().getResource("topic.xq"));
            topics.init(new ModuleContext(config,"topic"));
            Query introspect = new Query();
            modules.put("introspect",introspect);
            introspect.getMethodConfiguration("GET").setQuerySource(introspect.getClass().getResource("introspect.xq"));
            introspect.init(new ModuleContext(config,"introspect"));
         } catch (EXistException ex) {
            throw new ServletException("Exception during module init(): "+ex.getMessage(),ex);
         }
      }
      
      
      // XML lib checks....
      if( XmlLibraryChecker.isXercesVersionOK() ){
         LOG.info("Detected "+ XmlLibraryChecker.XERCESVERSION + ", OK.");
         
      } else {
         LOG.warn("eXist requires '"+ XmlLibraryChecker.XERCESVERSION
                 + "' but detected '"+ XmlLibraryChecker.getXercesVersion()
                 +"'. Please add the correct version to the "
                 +"class-path, e.g. in the 'endorsed' folder of "
                 +"the servlet container or in the 'endorsed' folder "
                 +"of the JRE.");
      }
      
      if( XmlLibraryChecker.isXalanVersionOK() ){
         LOG.info("Detected "+ XmlLibraryChecker.XALANVERSION + ", OK.");
         
      } else {
         LOG.warn("eXist requires '"+ XmlLibraryChecker.XALANVERSION
                 + "' but detected '"+ XmlLibraryChecker.getXalanVersion()
                 +"'. Please add the correct version to the "
                 +"class-path, e.g. in the 'endorsed' folder of "
                 +"the servlet container or in the 'endorsed' folder "
                 +"of the JRE.");
      }
   }
   
   protected void service(HttpServletRequest request, HttpServletResponse response)
   throws ServletException {
      try {
         // Get the path
         String path = request.getPathInfo();
         
         int firstSlash = path.indexOf('/',1);
         if (firstSlash<0 && path.length()==1) {
            response.sendError(400,"Module not specified.");
            return;
         }
         String moduleName = firstSlash<0 ? path.substring(1) : path.substring(1,firstSlash);
         path = firstSlash<0 ? "" : path.substring(firstSlash);
         
         AtomModule module = (AtomModule)modules.get(moduleName);
         if (module==null) {
            response.sendError(400,"Module "+moduleName+" not found.");
            return;
         }
         
         User user = null;
         if (noAuth.get(moduleName)==null) {
            // Authenticate
            user = authenticate(request,response);
            if (user == null) {
               // You now get a challenge if there is no user
               return;
            }
         }

         final Principal principal = new UserXmldbPrincipal(WebDAV.BASIC_AUTH,user);
         HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
            public Principal getUserPrincipal() {
               return principal;
            }
         };
         
         // Handle the resource
         DBBroker broker = null;
         try {
            broker = pool.get(user);
            module.process(broker,new HttpRequestMessage(request,path,'/'+moduleName),new HttpResponseMessage(response));
         } catch (NotFoundException ex) {
            LOG.info("Resource "+path+" not found by "+moduleName,ex);
            response.sendError(404,ex.getMessage());
         } catch (PermissionDeniedException ex) {
            LOG.info("Permission denied to "+path+" by "+moduleName+" for "+user.getName(),ex);
            response.sendError(401,ex.getMessage());
         } catch (BadRequestException ex) {
            LOG.info("Bad request throw from module "+moduleName,ex);
            response.sendError(400,ex.getMessage());
         } catch (EXistException ex) {
            LOG.fatal("Exception getting broker from pool for user "+user.getName(),ex);
            response.sendError(500,"Service is not available.");
         } finally {
            pool.release(broker);
         }
      } catch (IOException ex) {
         LOG.fatal("I/O exception on request.",ex);
         try {
            response.sendError(500,"Service is not available.");
         } catch (IOException finalEx) {
            LOG.fatal("Cannot return 500 on exception.",ex);
         }
      }
      
      
   }
   
    /* (non-Javadoc)
     * @see javax.servlet.GenericServlet#destroy()
     */
   public void destroy() {
      super.destroy();
      BrokerPool.stopAll(false);
   }
   
   private User authenticate(HttpServletRequest request,HttpServletResponse response)
   throws java.io.IOException {
      // First try to validate the principial if passed from the servlet engine
      Principal principal = request.getUserPrincipal();
      
      if (principal instanceof XmldbPrincipal){
         String username = ((XmldbPrincipal)principal).getName();
         String password = ((XmldbPrincipal)principal).getPassword();
         
         LOG.info("Validating Principle: " + principal.getName());
         User user = pool.getSecurityManager().getUser(username);
         
         if (user != null){
            if (password.equalsIgnoreCase(user.getPassword())){
               LOG.info("Valid User: " + user.getName());
               return user;
            } else {
               LOG.info( "Password invalid for user: " + username );
            }
            LOG.info("User not found: " + principal.getName());
         }
      }
      
      String auth = request.getHeader("Authorization");
      if (auth == null && defaultUser!=null) {
         return defaultUser;
      }
      return authenticator.authenticate(request,response);
   }
   
   private User getDefaultUser() {
      if (defaultUsername != null) {
         User user = pool.getSecurityManager().getUser(defaultUsername);
         if (user != null) {
            if (!user.validate(defaultPassword)) {
               return null;
            }
         }
         return user;
      }
      return null;
   }
   
   private void startup(Configuration configuration)
   throws ServletException {
      if ( configuration == null ) {
         throw new ServletException( "database has not been configured" );
      }
      LOG.info("configuring eXist instance");
      try {
         if ( !BrokerPool.isConfigured() ) {
            BrokerPool.configure( 1, 5, configuration );
         }
      } catch ( EXistException e ) {
         throw new ServletException( e.getMessage() );
      }
      try {
         LOG.info("registering XMLDB driver");
         Class clazz = Class.forName("org.exist.xmldb.DatabaseImpl");
         Database database = (Database)clazz.newInstance();
         DatabaseManager.registerDatabase(database);
      } catch (ClassNotFoundException e) {
         LOG.info("ERROR", e);
      } catch (InstantiationException e) {
         LOG.info("ERROR", e);
      } catch (IllegalAccessException e) {
         LOG.info("ERROR", e);
      } catch (XMLDBException e) {
         LOG.info("ERROR", e);
      }
   }
}
