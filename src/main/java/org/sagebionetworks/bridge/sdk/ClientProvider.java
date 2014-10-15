package org.sagebionetworks.bridge.sdk;

import org.sagebionetworks.bridge.sdk.models.SignInCredentials;
import org.sagebionetworks.bridge.sdk.models.UserSession;

public class ClientProvider {

    private UserSession session;
    private final AuthenticationApiCaller authApi;
    private final Config config;

    private ClientProvider(String host) {
        this.config = Config.valueOf(host);
        this.authApi = AuthenticationApiCaller.valueOf(this);
    }

    private ClientProvider() {
        this.config = Config.valueOf();
        this.authApi = AuthenticationApiCaller.valueOf(this);
    }
    
    public static ClientProvider valueOf(String host) {
        Preconditions.checkNotEmpty(host, "Host is null or empty");
        return new ClientProvider(host);
    }

    public static ClientProvider valueOf() {
        return new ClientProvider();
    }

    UserSession getSession() { return session; }
    String getSessionToken() { return isSignedIn() ? session.getSessionToken() : null; }
    Config getConfig() { return config; }
    
    void setSession(UserSession session) {
        assert session != null;
        this.session = session;
    }

    public boolean isSignedIn() {
        return session != null;
    }

    public void signIn() {
        session = authApi.signIn(Preconditions.checkNotNull(config.getAccountEmail()),
                Preconditions.checkNotNull(config.getAccountPassword()));
    }
    
    public void signIn(SignInCredentials signIn) {
        if (signIn == null) {
            throw new IllegalArgumentException("SignInCredentials object must not be null.");
        }
        session = authApi.signIn(signIn.getUsername(), signIn.getPassword());
    }

    public void signOut() {
        if (session == null) {
            throw new IllegalStateException("A User needs to be signed in to call this method.");
        }
        authApi.signOut(session);
        session = null;
    }

    public BridgeUserClient getClient() {
        if (session == null) {
            throw new IllegalStateException("A User needs to be signed in to call this method.");
        }
        return BridgeUserClient.valueOf(this);
    }

    public BridgeResearcherClient getResearcherClient() {
        if (session == null) {
            throw new IllegalStateException("A User needs to be signed in to call this method.");
        }
        return BridgeResearcherClient.valueOf(this);
    }
}