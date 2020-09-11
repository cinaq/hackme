package mendixsso.implementation.utils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.ISession;
import mendixsso.implementation.handlers.OpenIDHandler;
import mendixsso.proxies.constants.Constants;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mendixsso.implementation.handlers.OpenIDHandler.CALLBACK;
import static mendixsso.implementation.handlers.OpenIDHandler.OPENID_CLIENTSERVLET_LOCATION;

public class OpenIDUtils {

    private OpenIDUtils() {
    }

    private static final String ALPHA_CAPS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String LOCATION_HEADER_NAME = "location";
    private static final String NUM = "0123456789";
    private static final String SPL_CHARS = "!@#$%^&*_=+-/";

    private static final ILogNode LOG = Core.getLogger(Constants.getLogNode());
    private static final Pattern OPENID_UUID_REGEX = Pattern.compile("mxid2/id\\?id=(\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12})$");
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String getApplicationUrl(IMxRuntimeRequest req) {
        final String serverName = req.getHttpServletRequest().getServerName();
        if (serverName == null) {
            LOG.warn("Something went wrong while determining the server name from the request, defaulting to the application root URL.");
            return getDefaultAppRootUrl();
        }

        try {
            // Because the Mendix Cloud load balancers terminate SSL connections, it is not possible to determine
            // the original request scheme (whether it is http or https). Therefore we assume https for all connections
            // except localhost (to enable local development).
            final String scheme = serverName.toLowerCase().endsWith(".test") || "localhost".equalsIgnoreCase(serverName) ? HTTP : HTTPS;
            final int serverPort = req.getHttpServletRequest().getServerPort();
            // Ports 80 and 443 should be avoided, as they are the default, therefore we pass in -1
            final URI appUri = new URI(scheme, null, serverName, serverPort == 80 || serverPort == 443 ? -1 : serverPort,
                    "/", null, null);
            return appUri.toString();
        } catch (URISyntaxException e) {
            LOG.warn("Something went wrong while constructing the application URL, defaulting to the application root URL.", e);
            return getDefaultAppRootUrl();
        }
    }

    private static String getDefaultAppRootUrl() {
        return ensureEndsWithSlash(Core.getConfiguration().getApplicationRootUrl());
    }

    public static String extractUUID(String openID) {
        if (openID != null) {
            final Matcher m = OPENID_UUID_REGEX.matcher(openID);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    public static String getOpenID(String uuid) {
        return ensureEndsWithSlash(Constants.getMxID2_OpenIDPrefix()) + "id?id=" + uuid;
    }

    public static String getRedirectUri(IMxRuntimeRequest req) {
        return getApplicationUrl(req) + OPENID_CLIENTSERVLET_LOCATION + CALLBACK;
    }

    public static void redirectToIndex(IMxRuntimeRequest req, IMxRuntimeResponse resp, String continuation) {
        resp.setStatus(IMxRuntimeResponse.SEE_OTHER);

        //no continuation provided, use index
        if (continuation == null)
            resp.addHeader(LOCATION_HEADER_NAME, OpenIDHandler.INDEX_PAGE);
        else {
            if (continuation.trim().startsWith("javascript:")) {
                throw new IllegalArgumentException("Javascript injection detected!");
            } else if (!continuation.startsWith("http://") && !continuation.startsWith("https://")) {
                resp.addHeader(LOCATION_HEADER_NAME, getApplicationUrl(req) + continuation);
            } else {
                resp.addHeader(LOCATION_HEADER_NAME, continuation);
            }
        }
    }

    private static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String getFingerPrint(IMxRuntimeRequest req) {
        String agent = req.getHeader("User-Agent");
        if (agent != null)
            return base64Encode(agent.getBytes());

        return "";
    }

    public static String getFingerPrint(ISession session) {
        String agent = session.getUserAgent();
        if (agent != null)
            return base64Encode(agent.getBytes());

        return "";

    }

    public static String ensureEndsWithSlash(String text) {
        return text.endsWith("/") ? text : text + "/";
    }

    public static String randomStrongPassword(int minLen, int maxLen, int noOfCAPSAlpha,
                                              int noOfDigits, int noOfSplChars) {
        if (minLen > maxLen)
            throw new IllegalArgumentException("Min. Length > Max. Length!");
        if ((noOfCAPSAlpha + noOfDigits + noOfSplChars) > minLen)
            throw new IllegalArgumentException
                    ("Min. Length should be at least sum of (CAPS, DIGITS, SPL CHARS) Length!");

        int len = RANDOM.nextInt(maxLen - minLen + 1) + minLen;
        char[] pswd = new char[len];
        int index;
        for (int i = 0; i < noOfCAPSAlpha; i++) {
            index = getNextIndex(len, pswd);
            pswd[index] = ALPHA_CAPS.charAt(RANDOM.nextInt(ALPHA_CAPS.length()));
        }
        for (int i = 0; i < noOfDigits; i++) {
            index = getNextIndex(len, pswd);
            pswd[index] = NUM.charAt(RANDOM.nextInt(NUM.length()));
        }
        for (int i = 0; i < noOfSplChars; i++) {
            index = getNextIndex(len, pswd);
            pswd[index] = SPL_CHARS.charAt(RANDOM.nextInt(SPL_CHARS.length()));
        }
        for (int i = 0; i < len; i++) {
            if (pswd[i] == 0) {
                pswd[i] = ALPHA.charAt(RANDOM.nextInt(ALPHA.length()));
            }
        }
        return String.valueOf(pswd);
    }

    private static int getNextIndex(int len, char[] pswd) {
        int index;
        //noinspection StatementWithEmptyBody
        while (pswd[index = RANDOM.nextInt(len)] != 0) ;
        return index;
    }

    public static String convertInputStreamToString(InputStream is) {
        final Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
