package org.nmssever.util;

/**
 * The type Db constants.
 */
public class DBConstants {

    /**
     * The constant TABLE_CREDENTIAL_PROFILES.
     */
    public static final String TABLE_CREDENTIAL_PROFILES = "credentialProfiles";

    /**
     * The constant COL_CREDENTIAL_PROFILE_NAME.
     */
    public static final String COL_CREDENTIAL_PROFILE_NAME = "credential_profile_name";

    /**
     * The constant COL_SYSTEM_TYPE.
     */
    public static final String COL_SYSTEM_TYPE = "system_type";

    /**
     * The constant COL_CREDENTIAL_CONFIG.
     */
    public static final String COL_CREDENTIAL_CONFIG = "CredentialConfig";

    /**
     * The constant COL_USERNAME.
     */
    public static final String COL_USERNAME = "username";

    /**
     * The constant COL_PASSWORD.
     */
    public static final String COL_PASSWORD = "password";

    /**
     * The constant COL_PROTOCOL.
     */
    public static final String COL_PROTOCOL = "protocol";

    /**
     * The constant TABLE_DISCOVERY_PROFILES.
     */
    public static final String TABLE_DISCOVERY_PROFILES = "discoveryprofiles";

    /**
     * The constant COL_DISCOVERY_PROFILE_NAME.
     */
    public static final String COL_DISCOVERY_PROFILE_NAME = "discovery_profile_name";

    /**
     * The constant COL_IP.
     */
    public static final String COL_IP = "ip";

    /**
     * The constant COL_DISCOVERY_STATUS.
     */
    public static final String COL_DISCOVERY_STATUS = "discovery_status";

    /**
     * The constant COL_PROVISION_STATUS.
     */
    public static final String COL_PROVISION_STATUS = "provision_status";

    /**
     * The constant SELECT_CREDENTIAL_PROFILE.
     */
    public static final String SELECT_CREDENTIAL_PROFILE =
            "SELECT * FROM credentialprofiles WHERE id = $1";

    /**
     * The constant INSERT_CREDENTIAL_PROFILE.
     */
    public static final String INSERT_CREDENTIAL_PROFILE =
            "INSERT INTO credential_profiles (credential_profile_name, username, password, protocol) VALUES ($1, $2, $3, $4)";

    /**
     * The constant COL_ID.
     */
    public static final String COL_ID = "id";

    /**
     * The constant COL_CREDENTIALCONFIG.
     */
    public static final String COL_CREDENTIALCONFIG = "credentialconfig";

    /**
     * The constant UPDATE_CREDENTIAL_PROFILE.
     */
    public static final String UPDATE_CREDENTIAL_PROFILE =
            "UPDATE " + TABLE_CREDENTIAL_PROFILES + " " +
                    "SET " + COL_CREDENTIAL_PROFILE_NAME + " = $1, " +
                    COL_SYSTEM_TYPE + " = $2, " +
                    COL_CREDENTIALCONFIG + " = $3 " +
                    "WHERE " + COL_ID + " = $4";


    /**
     * The constant CHECK_CREDENTIAL_PROFILE_USAGE.
     */
    public static final String CHECK_CREDENTIAL_PROFILE_USAGE =
            "SELECT COUNT(*) FROM " + TABLE_DISCOVERY_PROFILES + " WHERE " + COL_CREDENTIAL_PROFILE_NAME + " = $1";


    /**
     * The constant DELETE_CREDENTIAL_PROFILE.
     */
    public static final String DELETE_CREDENTIAL_PROFILE =
            "DELETE FROM " + TABLE_CREDENTIAL_PROFILES + " WHERE id = $1";

    /**
     * The constant CHECK_CREDENTIAL_PROFILE_EXISTENCE.
     */
    public static final String CHECK_CREDENTIAL_PROFILE_EXISTENCE =
            "SELECT COUNT(*) FROM " + TABLE_CREDENTIAL_PROFILES + " WHERE " + COL_CREDENTIAL_PROFILE_NAME + " = $1";


    /**
     * The constant COL_CREDENTIAL_PROFILE_ID.
     */
    public static final String COL_CREDENTIAL_PROFILE_ID = "credential_profile_id";
    /**
     * The constant INSERT_DISCOVERY_PROFILE.
     */
    public static final String INSERT_DISCOVERY_PROFILE =
            "INSERT INTO " + TABLE_DISCOVERY_PROFILES + " (" +
                    COL_DISCOVERY_PROFILE_NAME + ", " + COL_IP + ", " + COL_CREDENTIAL_PROFILE_ID +
                    ") VALUES ($1, $2, $3) RETURNING id";


    /**
     * The constant COL_DISCOVERY_PROFILE_ID.
     */
    public static final String COL_DISCOVERY_PROFILE_ID = "id";

    /**
     * The constant SELECT_DISCOVERY_PROFILE.
     */
    public static final String SELECT_DISCOVERY_PROFILE =
            "SELECT * FROM " + TABLE_DISCOVERY_PROFILES +
                    " WHERE " + COL_DISCOVERY_PROFILE_ID + " = $1";


}
