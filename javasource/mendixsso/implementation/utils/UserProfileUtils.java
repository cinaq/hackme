package mendixsso.implementation.utils;

import com.mendix.systemwideinterfaces.core.IContext;
import com.nimbusds.jose.util.JSONObjectUtils;
import mendixsso.proxies.UserProfile;
import net.minidev.json.JSONObject;

import java.text.ParseException;

public class UserProfileUtils {

    private UserProfileUtils(){
    }

    private static final String MX_USER_PROFILE_V_1 = "mx:user:profile:v1";

    public static UserProfile getUserProfile(IContext context, String userInfoJSON) throws ParseException {
        final JSONObject userInfoJSONObject = JSONObjectUtils.parse(userInfoJSON);
        return getUserProfile(context, userInfoJSONObject);
    }

    public static UserProfile getUserProfile(IContext context, JSONObject userInfoJSONObject) throws ParseException {
        final JSONObject mxProfileClaim = JSONObjectUtils.getJSONObject(userInfoJSONObject, MX_USER_PROFILE_V_1);
        return extractUserProfileFromMxProfileClaim(context, mxProfileClaim);
    }

    private static UserProfile extractUserProfileFromMxProfileClaim(IContext context, JSONObject mxProfileClaim) {
        final UserProfile userProfile = new UserProfile(context);
        userProfile.setOpenId(mxProfileClaim.getAsString("openid2_id"));
        userProfile.setDisplayName(mxProfileClaim.getAsString("display_name"));
        userProfile.setAvatarThumbnailUrl(mxProfileClaim.getAsString("avatar_thumb_url"));
        userProfile.setAvatarUrl(mxProfileClaim.getAsString("avatar_url"));
        userProfile.setBio(mxProfileClaim.getAsString("bio"));
        userProfile.setWebsite(mxProfileClaim.getAsString("website"));
        userProfile.setEmailAddress(mxProfileClaim.getAsString("email"));
        userProfile.setPhone(mxProfileClaim.getAsString("phone_number"));
        userProfile.setJobTitle(mxProfileClaim.getAsString("job_title"));
        userProfile.setDepartment(mxProfileClaim.getAsString("job_department"));
        userProfile.setLocation(mxProfileClaim.getAsString("location"));
        userProfile.setCountry(mxProfileClaim.getAsString("country"));
        userProfile.setLinkedIn(mxProfileClaim.getAsString("social_linkedin"));
        userProfile.setTwitter(mxProfileClaim.getAsString("social_twitter"));
        userProfile.setSkype(mxProfileClaim.getAsString("social_skype"));
        userProfile.setCompanyId(mxProfileClaim.getAsString("company_id"));
        userProfile.setCompany(mxProfileClaim.getAsString("company_name"));
        return userProfile;
    }
}
