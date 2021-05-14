package de.dytanic.cloudnet.ext.rest;

import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerAuthentication;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerCluster;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerCommand;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerDatabase;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerGroups;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerLocalTemplate;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerLocalTemplateFileSystem;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerLogout;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerModules;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerPing;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerServices;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerShowOpenAPI;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerStatus;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerTasks;
import de.dytanic.cloudnet.ext.rest.http.V1SecurityProtectionHttpHandler;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerAuthorization;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerCluster;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerDatabase;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerGroups;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerInfo;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerLiveConsole;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerModule;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerService;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerServiceVersionProvider;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerSession;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerTasks;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerTemplate;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerTemplateStorages;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

public final class CloudNetRestModule extends NodeCloudNetModule {

    @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
    public void initHttpHandlers() {
        this.getHttpServer()
                // v2 rest auth
                .registerHandler("/api/v2/auth", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerAuthorization())
                // v2 session management
                .registerHandler("/api/v2/session/*", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerSession())
                // v2 node status check / information
                .registerHandler("/api/v2/info", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerInfo())
                // v2 node live console
                .registerHandler("/api/v2/liveConsole", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerLiveConsole("http.v2.live.console"))
                // v2 cluster
                .registerHandler("/api/v2/cluster", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerCluster("http.v2.cluster"))
                .registerHandler("/api/v2/cluster/{node}", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerCluster("http.v2.cluster"))
                .registerHandler("/api/v2/cluster/{node}/command", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerCluster("http.v2.cluster"))
                // v2 database
                .registerHandler("/api/v2/database", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerDatabase("http.v2.database"))
                .registerHandler("/api/v2/database/{name}", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerDatabase("http.v2.database"))
                .registerHandler("/api/v2/database/{name}/*", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerDatabase("http.v2.database"))
                // v2 groups
                .registerHandler("/api/v2/group", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerGroups("http.v2.groups"))
                .registerHandler("/api/v2/group/{group}", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerGroups("http.v2.groups"))
                .registerHandler("/api/v2/group/{group}/*", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerGroups("http.v2.groups"))
                // v2 tasks
                .registerHandler("/api/v2/task", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerTasks("http.v2.tasks"))
                .registerHandler("/api/v2/task/{task}", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerTasks("http.v2.tasks"))
                .registerHandler("/api/v2/task/{task}/*", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerTasks("http.v2.tasks"))
                // v2 services
                .registerHandler("/api/v2/service", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerService("http.v2.services"))
                .registerHandler("/api/v2/service/{identifier}", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerService("http.v2.services"))
                .registerHandler("/api/v2/service/{identifier}/*", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerService("http.v2.services"))
                // v2 template storage management
                .registerHandler("/api/v2/templateStorage", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerTemplateStorages("http.v2.template.storage"))
                .registerHandler("/api/v2/templateStorage/{storage}/*", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerTemplateStorages("http.v2.template.storage"))
                // v2 template management
                .registerHandler("/api/v2/template/{storage}/{prefix}/{name}/*", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerTemplate("http.v2.template"))
                // v2 server version management
                .registerHandler("/api/v2/serviceVersion", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerServiceVersionProvider("http.v2.service.provider"))
                .registerHandler("/api/v2/serviceVersion/{version}", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerServiceVersionProvider("http.v2.service.provider"))
                // v2 module management
                .registerHandler("/api/v2/module", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerModule("http.v2.module"))
                .registerHandler("/api/v2/module/{name}", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerModule("http.v2.module"))
                .registerHandler("/api/v2/module/{name}/*", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerModule("http.v2.module"))
                // legacy v1 handlers
                .registerHandler("/api/v1", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerShowOpenAPI())
                .registerHandler("/api/v1/*", IHttpHandler.PRIORITY_HIGH, new V1SecurityProtectionHttpHandler())
                .registerHandler("/api/v1/auth", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerAuthentication())
                .registerHandler("/api/v1/logout", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLogout())
                .registerHandler("/api/v1/ping", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerPing("cloudnet.http.v1.ping"))
                .registerHandler("/api/v1/status", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerStatus("cloudnet.http.v1.status"))
                .registerHandler("/api/v1/command", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerCommand("cloudnet.http.v1.command"))
                .registerHandler("/api/v1/modules", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerModules("cloudnet.http.v1.modules"))
                .registerHandler("/api/v1/cluster", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerCluster("cloudnet.http.v1.cluster"))
                .registerHandler("/api/v1/cluster/{node}", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerCluster("cloudnet.http.v1.cluster"))
                .registerHandler("/api/v1/services", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerServices("cloudnet.http.v1.services"))
                .registerHandler("/api/v1/services/{uuid}", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerServices("cloudnet.http.v1.services"))
                .registerHandler("/api/v1/services/{uuid}/{operation}", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerServices("cloudnet.http.v1.services.operation"))
                .registerHandler("/api/v1/tasks", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerTasks("cloudnet.http.v1.tasks"))
                .registerHandler("/api/v1/tasks/{name}", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerTasks("cloudnet.http.v1.tasks"))
                .registerHandler("/api/v1/groups", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerGroups("cloudnet.http.v1.groups"))
                .registerHandler("/api/v1/groups/{name}", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerGroups("cloudnet.http.v1.groups"))
                .registerHandler("/api/v1/db/{name}", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerDatabase("cloudnet.http.v1.database"))
                .registerHandler("/api/v1/db/{name}/{key}", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerDatabase("cloudnet.http.v1.database"))
                .registerHandler("/api/v1/local_templates", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLocalTemplate("cloudnet.http.v1.lt.list"))
                .registerHandler("/api/v1/local_templates/{prefix}/{name}", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLocalTemplate("cloudnet.http.v1.lt.template"))
                .registerHandler("/api/v1/local_templates/{prefix}/{name}/files", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLocalTemplateFileSystem("cloudnet.http.v1.lt.files"))
                .registerHandler("/api/v1/local_templates/{prefix}/{name}/files/*", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLocalTemplateFileSystem("cloudnet.http.v1.lt.files"))
        ;
    }
}