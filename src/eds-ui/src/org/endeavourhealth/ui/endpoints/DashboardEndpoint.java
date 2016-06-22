package org.endeavourhealth.ui.endpoints;

import org.endeavourhealth.core.data.admin.LibraryRepository;
import org.endeavourhealth.core.data.admin.models.ActiveItem;
import org.endeavourhealth.core.data.admin.models.Audit;
import org.endeavourhealth.core.data.admin.models.Item;
import org.endeavourhealth.ui.json.JsonFolderContent;
import org.endeavourhealth.ui.json.JsonRabbitNode;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.*;

@Path("/dashboard")
public final class DashboardEndpoint extends AbstractEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardEndpoint.class);
    private static final Random rnd = new Random();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/getRecentDocuments")
    public Response getRecentDocuments(@Context SecurityContext sc, @QueryParam("count") int count) throws Exception {
        super.setLogbackMarkers(sc);

        UUID userUuid = getEndUserUuidFromToken(sc);
        UUID orgUuid = getOrganisationUuidFromToken(sc);

        LOG.trace("getRecentDocuments {}", count);

        List<JsonFolderContent> ret = new ArrayList<>();

        LibraryRepository repository = new LibraryRepository();

        Iterable<Audit> audit = repository.getAuditByOrgAndDateDesc(orgUuid);
        for (Audit auditItem: audit) {
            Iterable<ActiveItem> activeItems = repository.getActiveItemByAuditId(auditItem.getId());
            for (ActiveItem activeItem: activeItems) {
                if (activeItem.getIsDeleted()!=null && activeItem.getIsDeleted()==false) {
                    Item item = repository.getItemByKey(activeItem.getItemId(), activeItem.getAuditId());

                    JsonFolderContent content = new JsonFolderContent(activeItem, item, auditItem);
                    ret.add(content);
                }
            }
        }

        clearLogbackMarkers();

        return Response
                .ok()
                .entity(ret)
                .build();
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/rabbitNodes")
    public Response getRabbitNodes(@Context SecurityContext sc) throws Exception {
        super.setLogbackMarkers(sc);
        // TODO : Extract to config
        List<JsonRabbitNode> ret = new ArrayList<>();
        ret.add(new JsonRabbitNode("localhost:15672", 0));
        ret.add(new JsonRabbitNode("localhost:15673", 0));
        ret.add(new JsonRabbitNode("localhost:15674", 0));

        clearLogbackMarkers();

        return Response
            .ok()
            .entity(ret)
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/rabbitNode/ping")
    public Response pingRabbitNode(@Context SecurityContext sc, @QueryParam("address") String address) throws Exception {
        super.setLogbackMarkers(sc);

        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("guest", "guest");
        Client client = ClientBuilder.newClient();
        client.register(feature);

        WebTarget resource = client.target("http://"+address+"/api/cluster-name");
        Invocation.Builder request = resource.request();

        int ping = -1;
        try {
            long startTime = System.currentTimeMillis();
            Response response = request.get();
            long endTime = System.currentTimeMillis();

            if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL)
                ping = Math.toIntExact(endTime - startTime);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        JsonRabbitNode ret = new JsonRabbitNode(address, ping);
        clearLogbackMarkers();

        return Response
            .ok()
            .entity(ret)
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/rabbitNode/queues")
    public Response getRabbitQueues(@Context SecurityContext sc, @QueryParam("address") String address) throws Exception {
        super.setLogbackMarkers(sc);

        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("guest", "guest");
        Client client = ClientBuilder.newClient();
        client.register(feature);

        address = "localhost:15672";
        WebTarget resource = client.target("http://"+address+"/api/queues");
        Invocation.Builder request = resource.request();

        Response response = request.get();
        String ret = null;
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            ret = response.readEntity(String.class);
        }

        clearLogbackMarkers();

        return Response
            .ok()
            .entity(ret)
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/rabbitNode/exchanges")
    public Response getRabbitExchanges(@Context SecurityContext sc, @QueryParam("address") String address) throws Exception {
        super.setLogbackMarkers(sc);

        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("guest", "guest");
        Client client = ClientBuilder.newClient();
        client.register(feature);

        address = "localhost:15672";
        WebTarget resource = client.target("http://"+address+"/api/exchanges");
        Invocation.Builder request = resource.request();

        Response response = request.get();
        String ret = null;
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            ret = response.readEntity(String.class);
        }

        clearLogbackMarkers();

        return Response
            .ok()
            .entity(ret)
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/rabbitNode/bindings")
    public Response getRabbitBindings(@Context SecurityContext sc, @QueryParam("address") String address) throws Exception {
        super.setLogbackMarkers(sc);

        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("guest", "guest");
        Client client = ClientBuilder.newClient();
        client.register(feature);

        address = "localhost:15672";
        WebTarget resource = client.target("http://"+address+"/api/bindings");
        Invocation.Builder request = resource.request();

        Response response = request.get();
        String ret = null;
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            ret = response.readEntity(String.class);
        }

        clearLogbackMarkers();

        return Response
            .ok()
            .entity(ret)
            .build();
    }
}
