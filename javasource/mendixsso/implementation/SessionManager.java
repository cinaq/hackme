package mendixsso.implementation;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.ISession;
import com.mendix.systemwideinterfaces.core.IUser;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import mendixsso.implementation.utils.MendixUtils;
import mendixsso.implementation.utils.OpenIDUtils;
import mendixsso.implementation.utils.TokenUtils;
import mendixsso.proxies.ForeignIdentity;
import mendixsso.proxies.PlatformSession;
import system.proxies.Session;
import system.proxies.User;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static mendixsso.proxies.constants.Constants.getLogNode;

public class SessionManager {

    private SessionManager() {
    }

    private static final ILogNode LOG = Core.getLogger(getLogNode());

    private static final String XASID_COOKIE = "XASID";
    private static final String XAS_SESSION_ID = Core.getConfiguration().getSessionIdCookieName();
    private static final String SID_CLAIM_NAME = "sid";

    public static void createSessionForUser(IContext context, IMxRuntimeResponse resp, IMxRuntimeRequest req,
                                            User user, OIDCTokenResponse oidcTokenResponse) throws CoreException, ParseException, java.text.ParseException {

        LOG.info("User " + user.getName() + " authenticated. Starting session..");

        final IUser iUser = Core.getUser(context, user.getName());
        final ISession session = Core.initializeSession(iUser, null);

        final JWTClaimsSet idTokenClaimsSet = TokenUtils.persistTokens(context, oidcTokenResponse, user, session);
        if (idTokenClaimsSet != null) {
            final String sid = idTokenClaimsSet.getStringClaim(SID_CLAIM_NAME);
            if (sid != null) {
                createPlatformSession(context, session, sid);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Created session, fingerprint: " + OpenIDUtils.getFingerPrint(session));
        }

        writeSessionCookies(session, resp);
    }

    public static void writeSessionCookies(ISession session,
                                           IMxRuntimeResponse resp) {
        resp.addCookie(XAS_SESSION_ID, session.getId().toString(), "/", "", -1, true);
        resp.addCookie(XASID_COOKIE, "0." + Core.getXASId(), "/", "", -1, true);
    }

    public static void logoutAllSessionsOfForeignIdentity(IContext context, String foreignIdentityUUID) {
        final List<Session> sessions = retrieveSessions(context, foreignIdentityUUID);
        sessions.forEach(SessionManager::logoutSession);
    }

    public static void logoutSession(IContext context, String platformSessionId) throws CoreException {
        final PlatformSession platformSession = retrievePlatformSession(context, platformSessionId);
        if (platformSession != null) {
            logoutSession(platformSession.getPlatformSession_Session());
        }
    }

    private static void logoutSession(Session sessionObject) {
        if (sessionObject != null) {
            final ISession session = Core.getSessionById(UUID.fromString(sessionObject.getSessionId()));
            Core.logout(session);
        }
    }

    private static PlatformSession retrievePlatformSession(IContext context, String platformSessionId) {
        return MendixUtils.retrieveSingleObjectFromDatabase(context, PlatformSession.class, "//%s[%s = $platformSessionId]",
                new HashMap<String, Object>() {{
                    put("platformSessionId", platformSessionId);
                }},
                PlatformSession.entityName,
                PlatformSession.MemberNames.SessionId.toString()
        );
    }

    private static void createPlatformSession(IContext context, ISession session, String sid) throws CoreException {
        final PlatformSession platformSession = new PlatformSession(context);
        platformSession.setSessionId(sid);
        platformSession.setPlatformSession_Session(Session.initialize(context, session.getMendixObject()));
        platformSession.commit();
    }

    private static List<Session> retrieveSessions(IContext context, String foreignIdentityUUID) {
        return MendixUtils.retrieveFromDatabase(context, Session.class, "//%s[%s/%s/%s/%s/%s = $uuid]",
                new HashMap<String, Object>() {{
                    put("uuid", foreignIdentityUUID);
                }},
                Session.entityName,
                Session.MemberNames.Session_User.toString(),
                User.entityName,
                ForeignIdentity.MemberNames.ForeignIdentity_User.toString(),
                ForeignIdentity.entityName,
                ForeignIdentity.MemberNames.UUID.toString()
        );
    }
}
