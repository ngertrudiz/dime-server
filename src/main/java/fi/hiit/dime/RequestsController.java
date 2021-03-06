/*
  Copyright (c) 2016-2017 University of Helsinki

  Permission is hereby granted, free of charge, to any person
  obtaining a copy of this software and associated documentation files
  (the "Software"), to deal in the Software without restriction,
  including without limitation the rights to use, copy, modify, merge,
  publish, distribute, sublicense, and/or sell copies of the Software,
  and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be
  included in all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
*/

package fi.hiit.dime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hiit.dime.data.Profile;
import fi.hiit.dime.xdi.XdiService;
import xdi2.client.XDIClientRoute;
import xdi2.client.exceptions.Xdi2ClientException;
import xdi2.client.impl.XDIAbstractClient;
import xdi2.client.impl.local.XDILocalClient;
import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.bootstrap.XDIBootstrap;
import xdi2.core.constants.XDILinkContractConstants;
import xdi2.core.features.aggregation.Aggregation;
import xdi2.core.features.index.Index;
import xdi2.core.features.linkcontracts.instance.ConnectLinkContract;
import xdi2.core.features.linkcontracts.instance.LinkContract;
import xdi2.core.features.linkcontracts.instance.RootLinkContract;
import xdi2.core.features.nodetypes.XdiEntityCollection;
import xdi2.core.features.nodetypes.XdiPeerRoot;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.util.CopyUtil;
import xdi2.core.util.XDIAddressUtil;
import xdi2.core.util.iterators.EmptyIterator;
import xdi2.core.util.iterators.ReadOnlyIterator;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.constants.XDIMessagingConstants;
import xdi2.messaging.container.MessagingContainer;
import xdi2.messaging.operations.Operation;
import xdi2.messaging.response.FullMessagingResponse;
import xdi2.messaging.response.MessagingResponse;

/**
 * Requests API controller.
 *
 * @author Markus Sabadello, markus@danubetech.com
 */
@RestController
@RequestMapping("/api/requests")
public class RequestsController extends AuthorizedController {

    private static final Logger LOG = LoggerFactory.getLogger(RequestsController.class);

    RequestsController() {
    }

    /**
       @api {post} /requests/send/:target Sends a new link contact request to target
       @apiName Post
       @apiDescription Sends a new XDI link contract request to target DID.
       @apiParam {String} target The target's XDI address, e.g. "=!:did:sov:Vt8pgSZpGSRdD2fr3TBop9"

       @apiPermission user
       @apiGroup Requests
       @apiVersion 1.0.0
    */
    @RequestMapping(value="/send/{target}", method = RequestMethod.POST)
    public ResponseEntity<String>
        requestsSend(Authentication auth, @PathVariable String target)
        throws NotFoundException, BadRequestException
    {
        Profile profile = XdiService.get().profileDAO.profilesForUser(getUser(auth).getId()).get(0);
        XDIAddress senderXDIAddress = XdiService.getProfileDidXDIAddress(profile);
        XDIAddress targetXDIAddress = XDIAddress.create(target);

        // find XDI route

        XDIClientRoute<?> route;

        try {

            route = XdiService.get().getXDIAgent().route(XdiPeerRoot.createPeerRootXDIArc(targetXDIAddress));
        } catch (Exception ex) {

            throw new RuntimeException(ex.getMessage(), ex);
        }

        // build XDI message

        Message message = route.createMessage(senderXDIAddress, -1);
        message.setFromXDIAddress(senderXDIAddress);
        message.setToXDIAddress(targetXDIAddress);
        message.setLinkContractClass(ConnectLinkContract.class);
        message.setParameter(XDIMessagingConstants.XDI_ADD_MESSAGE_PARAMETER_MSG, Boolean.TRUE);
        Operation operation = message.createConnectOperation(XDIBootstrap.GET_LINK_CONTRACT_TEMPLATE_ADDRESS);
        operation.setVariableValue(XDIArc.create("{$get}"), XDIAddressUtil.concatXDIAddresses(targetXDIAddress, XDIAddress.create("#dime")));

        // send to XDI target

        XDIAbstractClient<?> client = (XDIAbstractClient<?>) route.constructXDIClient();
        MessagingResponse messagingResponse;

        try {

            /*                      RSASignatureCreator signatureCreator = new RSAGraphPrivateKeySignatureCreator(XdiService.get().myGraph(getUser(auth)));
                                    SigningManipulator manipulator = new SigningManipulator();
                                    manipulator.setSignatureCreator(signatureCreator);
                                    client.getManipulators().addManipulator(manipulator);*/

            messagingResponse = client.send(message.getMessageEnvelope());
        } catch (Xdi2ClientException ex) {

            throw new RuntimeException(ex.getMessage(), ex);
        }

        for (LinkContract pushLinkContract : FullMessagingResponse.getDeferredPushLinkContracts(messagingResponse)) {

            // write push link contract and index into graph

            CopyUtil.copyContextNode(pushLinkContract.getContextNode(), XdiService.get().myGraph(profile), null);
            XdiEntityCollection xdiLinkContractIndex = Index.getEntityIndex(XdiService.get().myGraph(profile), XDILinkContractConstants.XDI_ARC_CONTRACT, true);
            Index.setEntityIndexAggregation(xdiLinkContractIndex, pushLinkContract.getXdiEntity().getXDIAddress());
        }

        // done

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    /**
       @api {get} /requests/view View incoming link contracts
       @apiName Get
       @apiDescription Views pending incoming XDI link contract requests

       @apiExample {json} Example of output
       [
	{
    	"address": "=!:did:sov:2xpL6mwDZU5n7ZGMuXXDU9[$msg]@~0",
    	"from": "=!:did:sov:2xpL6mwDZU5n7ZGMuXXDU9",
    	"to": "=!:did:sov:Vt8pgSZpGSRdD2fr3TBop9",
    	"operation": "$connect",
    	"operationTarget": "$get{$contract}",
    	"operationVariables": {
        	"{$get}": [
            	"=!:did:sov:Vt8pgSZpGSRdD2fr3TBop9#dime"
        	]
    	}
	}
        ]

       @apiPermission user
       @apiGroup Requests
       @apiVersion 1.0.0
    */
    @RequestMapping(value="/view", method = RequestMethod.GET)
    public ResponseEntity<List<XdiRequest>>
        requestsView(Authentication auth)
        throws NotFoundException, BadRequestException
    {
        List<XdiRequest> result = new ArrayList<XdiRequest> ();

        for (Profile profile : XdiService.get().profileDAO.profilesForUser(getUser(auth).getId())) {

            // look in XDI graph
        
            Graph graph = XdiService.get().myGraph(profile);
            if (graph == null) continue;

            ContextNode requestsContextNode = graph.getDeepContextNode(XDIAddress.create("[$msg]"));
            ReadOnlyIterator<ContextNode> requestContextNodes = requestsContextNode == null ? new EmptyIterator<ContextNode> () : Aggregation.getAggregationContextNodes(requestsContextNode);

            // result

            for (ContextNode requestContextNode : requestContextNodes) {

                Message requestMessage = Message.fromContextNode(requestContextNode);
                Profile fromProfile = XdiService.findProfileByDidXDIAddress(requestMessage.getFromXDIAddress());
                Profile toProfile = XdiService.findProfileByDidXDIAddress(requestMessage.getToXDIAddress());
                String address = requestMessage.getContextNode().getXDIAddress().toString();
                String from = requestMessage.getFromXDIAddress().toString();
                String to = requestMessage.getToXDIAddress().toString();
                String profileName = fromProfile != null ? fromProfile.name : (toProfile != null ? toProfile.name : null);
                String direction = fromProfile != null ? "outgoing" : "incoming";
                String operation = "" + requestMessage.getOperations().next().getOperationXDIAddress();
                String operationTarget = "" + requestMessage.getOperations().next().getTargetXDIAddress();
                Map<String, Object> operationVariables = new HashMap<String, Object> ();

                for (Entry<XDIArc, Object> variableValue : requestMessage.getOperations().next().getVariableValues().entrySet()) {

                    String key = variableValue.getKey().toString();
                    Object value;
                                        
                    if (variableValue.getValue() instanceof List) {
                                                
                        List<String> valueList = new ArrayList<String> ();
                        for (Object item : ((List<?>) variableValue.getValue())) valueList.add("" + item);
                        value = valueList;
                    } else {
                                                
                        value = "" + variableValue.getValue();
                    }
                                        
                    operationVariables.put(key, value);
                }

                result.add(new XdiRequest(
                                          address, 
                                          from,
                                          to,
                                          profileName,
                                          direction,
                                          operation,
                                          operationTarget,
                                          operationVariables));
            }
        }

        // done

        return new ResponseEntity<List<XdiRequest>>(result, HttpStatus.OK);
    }

    /**
       @api {post} /requests/approve/:address Approve a pending incoming link contract request
       @apiName Approve
       @apiDescription Approves a pending incoming XDI link contract request
       @apiParam {String} address XDI address of request, e.g., =!:did:sov:2xpL6mwDZU5n7ZGMuXXDU9[$msg]@~0

       @apiPermission user
       @apiGroup Requests
       @apiVersion 1.0.0
    */
    @RequestMapping(value="/approve/{address}", method = RequestMethod.POST)
    public ResponseEntity<String>
        requestsApprove(Authentication auth, @PathVariable String address)
        throws NotFoundException, BadRequestException
    {
        XDIAddress XDIaddress = XDIAddress.create(address);
        boolean found = false;

        for (Profile profile : XdiService.get().profileDAO.profilesForUser(getUser(auth).getId())) {

            XDIAddress didXDIAddress = XdiService.getProfileDidXDIAddress(profile);

            if (didXDIAddress == null)
                continue;

            // look in XDI graph

            Graph graph = XdiService.get().myGraph(profile);
            ContextNode requestContextNode = graph.getDeepContextNode(XDIaddress);
            Message requestMessage = requestContextNode == null ? null : Message.fromContextNode(requestContextNode);

            if (requestMessage == null) continue; else found = true;

            // XDI request to local messaging container

            try {

                MessagingContainer messagingContainer = XdiService.get().myMessagingContainer(profile);
                MessageEnvelope messageEnvelope = new MessageEnvelope();
                Message message = messageEnvelope.createMessage(didXDIAddress, -1);
                message.setFromXDIAddress(didXDIAddress);
                message.setToXDIAddress(didXDIAddress);
                message.setLinkContractClass(RootLinkContract.class);
                message.createSendOperation(requestMessage);
                message.createDelOperation(XDIaddress);

                XDILocalClient client = new XDILocalClient(messagingContainer);
                client.send(messageEnvelope);
            } catch (Xdi2ClientException ex) {

                LOG.error("Cannot execute local XDI message: " + ex.getMessage(), ex);
                return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // done

        return found ? new ResponseEntity<String>(HttpStatus.NO_CONTENT) : new ResponseEntity<String>(HttpStatus.NOT_FOUND);
    }

    /**
       @api {post} /requests/delete Delete a pending incoming XDI link contract request
       @apiName Delete
       @apiDescription Deletes a pending incoming XDI link contract request
       @apiParam {String} address The address of the request (URL encoded)

       @apiPermission user
       @apiGroup Requests
       @apiVersion 1.0.0
    */
    @RequestMapping(value="/delete", method = RequestMethod.POST)
    public ResponseEntity<String>
        requestsDelete(Authentication auth, @RequestParam String address)
        throws NotFoundException, BadRequestException
    {
        XDIAddress XDIaddress = XDIAddress.create(address);

        for (Profile profile : XdiService.get().profileDAO.profilesForUser(getUser(auth).getId())) {

            XDIAddress didXDIAddress = XdiService.getProfileDidXDIAddress(profile);

            if (didXDIAddress == null)
                continue;

            // XDI request to local messaging container

            try {

                MessagingContainer messagingContainer = XdiService.get().myLocalMessagingContainer(profile);
                MessageEnvelope messageEnvelope = new MessageEnvelope();
                Message message = messageEnvelope.createMessage(didXDIAddress, -1);
                message.setFromXDIAddress(didXDIAddress);
                message.setToXDIAddress(didXDIAddress);
                message.setLinkContractClass(RootLinkContract.class);
                message.createDelOperation(XDIaddress);

                XDILocalClient client = new XDILocalClient(messagingContainer);
                client.send(messageEnvelope);
            } catch (Xdi2ClientException ex) {

                LOG.error("Cannot execute local XDI message: " + ex.getMessage(), ex);
                return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // done

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    private static class XdiRequest {
        public String address;
        public String from;
        public String to;
        public String profileName;
        public String direction;
        public String operation;
        public String operationTarget;
        public Map<String, Object> operationVariables;
        public XdiRequest(String address, String from, String to, String profileName, String direction, String operation, String operationTarget, Map<String, Object> operationVariables) { this.address = address; this.from = from; this.to = to; this.profileName = profileName; this.direction = direction; this.operation = operation; this.operationTarget = operationTarget; this.operationVariables = operationVariables; } 
    }
}
