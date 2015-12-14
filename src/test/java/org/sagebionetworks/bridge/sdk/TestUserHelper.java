package org.sagebionetworks.bridge.sdk;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.LocalDate;

import org.sagebionetworks.bridge.Tests;
import org.sagebionetworks.bridge.sdk.exceptions.ConsentRequiredException;
import org.sagebionetworks.bridge.sdk.models.subpopulations.SubpopulationGuid;
import org.sagebionetworks.bridge.sdk.models.subpopulations.SubpopulationGuidImpl;
import org.sagebionetworks.bridge.sdk.models.users.ConsentSignature;
import org.sagebionetworks.bridge.sdk.models.users.SharingScope;
import org.sagebionetworks.bridge.sdk.models.users.SignInCredentials;
import org.sagebionetworks.bridge.sdk.models.users.SignUpByAdmin;

import com.google.common.collect.Sets;

public class TestUserHelper {

    public static final String PASSWORD = "P4ssword";
    
    public static class TestUser {
        private final AdminClient adminClient;
        private final Session userSession;
        private final String username;
        private final String email;
        private final String password;
        private final Set<Roles> roles;

        public TestUser(AdminClient client, Session userSession, String username, String email, String password,
                Set<Roles> roleList) {

            this.adminClient = client;
            this.userSession = userSession;
            this.username = username;
            this.email = email;
            this.password = password;
            this.roles = (roleList == null) ? new HashSet<Roles>() : roleList;
            roles.add(Roles.TEST_USERS);
        }
        public Session getSession() {
            return userSession;
        }
        public String getUsername() {
            return username;
        }
        public String getEmail() {
            return email;
        }
        public String getPassword() {
            return password;
        }
        public Set<Roles> getRoles() {
            return roles;
        }
        public SubpopulationGuid getDefaultSubpopulation() {
            return new SubpopulationGuidImpl(Tests.TEST_KEY);
        }
        public boolean signOutAndDeleteUser() {
            userSession.signOut();
            return adminClient.deleteUser(email);
        }
        public boolean isSignedIn() {
            return userSession.isSignedIn();
        }
        public SignInCredentials getSignInCredentials() {
            return new SignInCredentials(Tests.TEST_KEY, email, PASSWORD);
        }
    }

    public static TestUser getSignedInAdmin() {
        Config config = ClientProvider.getConfig();
        Session session = ClientProvider.signIn(config.getAdminCredentials());
        AdminClient adminClient = session.getAdminClient();

        return new TestUserHelper.TestUser(adminClient, session, "", "", "", Sets.newHashSet(Roles.ADMIN));
    }
    
    public static TestUser createAndSignInUser(Class<?> cls, boolean consent, Roles... roles) {
        checkNotNull(cls);

        ClientProvider.setClientInfo(Tests.TEST_CLIENT_INFO);

        Config config = ClientProvider.getConfig();
        Session session = ClientProvider.signIn(config.getAdminCredentials());
        AdminClient adminClient = session.getAdminClient();

        Set<Roles> rolesList = (roles == null) ? Sets.<Roles>newHashSet() : Sets.newHashSet(roles);
        rolesList.add(Roles.TEST_USERS);
        String name = makeUserName(cls);

        // For email address, we don't want consent emails to bounce or SES will get mad at us. All test user email
        // addresses should be in the form bridge-testing+[semi-unique token]@sagebase.org. This directs all test
        // email to bridge-testing@sagebase.org.
        String emailAddress = makeEmail(name);

        SignUpByAdmin signUp = new SignUpByAdmin(name, emailAddress, PASSWORD, rolesList, consent);
        adminClient.createUser(signUp);

        Session userSession = null;
        try {
            SignInCredentials signIn = new SignInCredentials(Tests.TEST_KEY, name, PASSWORD);
            userSession = ClientProvider.signIn(signIn);
        } catch(ConsentRequiredException e) {
            userSession = e.getSession();
            if (consent) {
                ConsentSignature signature = new ConsentSignature("Tester", LocalDate.parse("1970-02-02"), null, null);
                userSession.getUserClient().consentToResearch(new SubpopulationGuidImpl(Tests.TEST_KEY), signature,
                        SharingScope.NO_SHARING);
            }
        }
        return new TestUserHelper.TestUser(adminClient, userSession, signUp.getUsername(), signUp.getEmail(),
                signUp.getPassword(), rolesList);
    }

    public static String makeUserName(Class<?> cls) {
        Config config = ClientProvider.getConfig();
        String devName = config.getDevName();
        String clsPart = cls.getSimpleName();
        String rndPart = RandomStringUtils.randomAlphabetic(4);
        return String.format("%s-%s-%s", devName, clsPart, rndPart);
    }
    
    public static String makeEmail(String userName) {
        return String.format("bridge-testing+%s@sagebase.org", userName);
    }
}
