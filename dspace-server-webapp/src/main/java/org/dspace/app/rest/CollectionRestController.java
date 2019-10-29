/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.repository.CollectionRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This RestController takes care of the creation and deletion of Collection's nested objects
 * This class will typically receive the UUID of a Collection and it'll perform logic on its nested objects
 */
@RestController
@RequestMapping("/api/" + CollectionRest.CATEGORY + "/" + CollectionRest.PLURAL_NAME)
public class CollectionRestController {

    @Autowired
    private Utils utils;

    @Autowired
    private CollectionRestRepository collectionRestRepository;

    @Autowired
    private CollectionService collectionService;

    /**
     * This method will create an Item and add it as a template to a Collection.
     *
     * Example:
     * <pre>
     * {@code
     * curl http://<dspace.server.url>/api/core/collections/51715dd3-5590-49f2-b227-6a663c849921/itemtemplate
     *  -XPOST -H 'Content-Type: Content-Type:application/json' \
     *  -H 'Authorization: Bearer eyJhbGciOiJI...' \
     *  --data '{
     *        "metadata": {
     *          "dc.type": [
     *            {
     *              "value": "Journal Article",
     *              "language": "en",
     *              "authority": null,
     *              "confidence": -1
     *            }
     *          ]
     *        },
     *        "inArchive": false,
     *        "discoverable": false,
     *        "withdrawn": false,
     *        "type": "item"
     *      }'
     * }
     * </pre>
     * @param request   The request as described above
     * @param uuid      The UUID of the Collection for which the template item should be made
     * @return          The created template
     * @throws SQLException
     * @throws AuthorizeException
     */
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
    @RequestMapping(method = RequestMethod.POST,
            value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/itemtemplate")
    public ResponseEntity<ResourceSupport> createTemplateItem(HttpServletRequest request, @PathVariable UUID uuid)
            throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = getCollection(context, uuid);
        ItemRest templateItem = collectionRestRepository.createTemplateItem(context, collection);
        context.commit();

        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null,
                new ItemResource(templateItem, utils));
    }

    /**
     * This method gets the template Item based on the owning Collection
     *
     * <pre>
     * {@code
     * curl http://<dspace.server.url>/api/core/collections/51715dd3-5590-49f2-b227-6a663c849921/itemtemplate
     *  -XGET \
     *  -H 'Authorization: Bearer eyJhbGciOiJI...'
     * }
     * </pre>
     * @param request
     * @param uuid      The UUID of the Collection from which you want the template item
     * @return          The template item from the Collection in the request
     * @throws SQLException
     */
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'READ')")
    @RequestMapping(method = RequestMethod.GET,
            value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/itemtemplate")
    public ItemResource getTemplateItem(HttpServletRequest request, @PathVariable UUID uuid)
            throws SQLException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = getCollection(context, uuid);
        ItemRest templateItem = collectionRestRepository.getTemplateItem(collection);

        return new ItemResource(templateItem, utils);
    }

    private Collection getCollection(Context context, UUID uuid) throws SQLException {
        Collection collection = collectionService.find(context, uuid);
        if (collection == null) {
            throw new ResourceNotFoundException(
                "The given uuid did not resolve to a collection on the server: " + uuid);
        }
        return collection;
    }
}
