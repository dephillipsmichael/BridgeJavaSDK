package org.sagebionetworks.bridge.rest.exceptions;

@SuppressWarnings("serial")
public class EntityNotFoundException extends BridgeSDKException {

    public EntityNotFoundException(String message, String endpoint) {
        super(message, 404, endpoint);
    }

}
