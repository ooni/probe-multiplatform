package org.ooni.engine.models

import org.ooni.probe.data.models.SettingsKey

enum class WebConnectivityCategory(
    val code: String,
    val iconKey: String,
    val titleKey: String,
    val descriptionKey: String,
    val settingsKey: SettingsKey?,
) {
    ANON(
        code = "ANON",
        iconKey = "category_anon",
        titleKey = "CategoryCode_ANON_Name",
        descriptionKey = "CategoryCode_ANON_Description",
        settingsKey = SettingsKey.ANON,
    ),

    COMT(
        code = "COMT",
        iconKey = "category_comt",
        titleKey = "CategoryCode_COMT_Name",
        descriptionKey = "CategoryCode_COMT_Description",
        settingsKey = SettingsKey.COMT,
    ),

    CTRL(
        code = "CTRL",
        iconKey = "category_ctrl",
        titleKey = "CategoryCode_CTRL_Name",
        descriptionKey = "CategoryCode_CTRL_Description",
        settingsKey = SettingsKey.CTRL,
    ),

    CULTR(
        code = "CULTR",
        iconKey = "category_cultr",
        titleKey = "CategoryCode_CULTR_Name",
        descriptionKey = "CategoryCode_CULTR_Description",
        settingsKey = SettingsKey.CULTR,
    ),

    ALDR(
        code = "ALDR",
        iconKey = "category_aldr",
        titleKey = "CategoryCode_ALDR_Name",
        descriptionKey = "CategoryCode_ALDR_Description",
        settingsKey = SettingsKey.ALDR,
    ),

    COMM(
        code = "COMM",
        iconKey = "category_comm",
        titleKey = "CategoryCode_COMM_Name",
        descriptionKey = "CategoryCode_COMM_Description",
        settingsKey = SettingsKey.COMM,
    ),

    ECON(
        code = "ECON",
        iconKey = "category_econ",
        titleKey = "CategoryCode_ECON_Name",
        descriptionKey = "CategoryCode_ECON_Description",
        settingsKey = SettingsKey.ECON,
    ),

    ENV(
        code = "ENV",
        iconKey = "category_env",
        titleKey = "CategoryCode_ENV_Name",
        descriptionKey = "CategoryCode_ENV_Description",
        settingsKey = SettingsKey.ENV,
    ),

    FILE(
        code = "FILE",
        iconKey = "category_file",
        titleKey = "CategoryCode_FILE_Name",
        descriptionKey = "CategoryCode_FILE_Description",
        settingsKey = SettingsKey.FILE,
    ),

    GMB(
        code = "GMB",
        iconKey = "category_gmb",
        titleKey = "CategoryCode_GMB_Name",
        descriptionKey = "CategoryCode_GMB_Description",
        settingsKey = SettingsKey.GMB,
    ),

    GAME(
        code = "GAME",
        iconKey = "category_game",
        titleKey = "CategoryCode_GAME_Name",
        descriptionKey = "CategoryCode_GAME_Description",
        settingsKey = SettingsKey.GAME,
    ),

    GOVT(
        code = "GOVT",
        iconKey = "category_govt",
        titleKey = "CategoryCode_GOVT_Name",
        descriptionKey = "CategoryCode_GOVT_Description",
        settingsKey = SettingsKey.GOVT,
    ),

    HACK(
        code = "HACK",
        iconKey = "category_hack",
        titleKey = "CategoryCode_HACK_Name",
        descriptionKey = "CategoryCode_HACK_Description",
        settingsKey = SettingsKey.HACK,
    ),

    HATE(
        code = "HATE",
        iconKey = "category_hate",
        titleKey = "CategoryCode_HATE_Name",
        descriptionKey = "CategoryCode_HATE_Description",
        settingsKey = SettingsKey.HATE,
    ),

    HOST(
        code = "HOST",
        iconKey = "category_host",
        titleKey = "CategoryCode_HOST_Name",
        descriptionKey = "CategoryCode_HOST_Description",
        settingsKey = SettingsKey.HOST,
    ),

    HUMR(
        code = "HUMR",
        iconKey = "category_humr",
        titleKey = "CategoryCode_HUMR_Name",
        descriptionKey = "CategoryCode_HUMR_Description",
        settingsKey = SettingsKey.HUMR,
    ),

    IGO(
        code = "IGO",
        iconKey = "category_igo",
        titleKey = "CategoryCode_IGO_Name",
        descriptionKey = "CategoryCode_IGO_Description",
        settingsKey = SettingsKey.IGO,
    ),

    LGBT(
        code = "LGBT",
        iconKey = "category_lgbt",
        titleKey = "CategoryCode_LGBT_Name",
        descriptionKey = "CategoryCode_LGBT_Description",
        settingsKey = SettingsKey.LGBT,
    ),

    MMED(
        code = "MMED",
        iconKey = "category_mmed",
        titleKey = "CategoryCode_MMED_Name",
        descriptionKey = "CategoryCode_MMED_Description",
        settingsKey = SettingsKey.MMED,
    ),

    NEWS(
        code = "NEWS",
        iconKey = "category_news",
        titleKey = "CategoryCode_NEWS_Name",
        descriptionKey = "CategoryCode_NEWS_Description",
        settingsKey = SettingsKey.NEWS,
    ),

    DATE(
        code = "DATE",
        iconKey = "category_date",
        titleKey = "CategoryCode_DATE_Name",
        descriptionKey = "CategoryCode_DATE_Description",
        settingsKey = SettingsKey.DATE,
    ),

    POLR(
        code = "POLR",
        iconKey = "category_polr",
        titleKey = "CategoryCode_POLR_Name",
        descriptionKey = "CategoryCode_POLR_Description",
        settingsKey = SettingsKey.POLR,
    ),

    PORN(
        code = "PORN",
        iconKey = "category_porn",
        titleKey = "CategoryCode_PORN_Name",
        descriptionKey = "CategoryCode_PORN_Description",
        settingsKey = SettingsKey.PORN,
    ),

    PROV(
        code = "PROV",
        iconKey = "category_prov",
        titleKey = "CategoryCode_PROV_Name",
        descriptionKey = "CategoryCode_PROV_Description",
        settingsKey = SettingsKey.PROV,
    ),

    PUBH(
        code = "PUBH",
        iconKey = "category_pubh",
        titleKey = "CategoryCode_PUBH_Name",
        descriptionKey = "CategoryCode_PUBH_Description",
        settingsKey = SettingsKey.PUBH,
    ),

    REL(
        code = "REL",
        iconKey = "category_rel",
        titleKey = "CategoryCode_REL_Name",
        descriptionKey = "CategoryCode_REL_Description",
        settingsKey = SettingsKey.REL,
    ),

    SRCH(
        code = "SRCH",
        iconKey = "category_srch",
        titleKey = "CategoryCode_SRCH_Name",
        descriptionKey = "CategoryCode_SRCH_Description",
        settingsKey = SettingsKey.SRCH,
    ),

    XED(
        code = "XED",
        iconKey = "category_xed",
        titleKey = "CategoryCode_XED_Name",
        descriptionKey = "CategoryCode_XED_Description",
        settingsKey = SettingsKey.XED,
    ),

    GRP(
        code = "GRP",
        iconKey = "category_grp",
        titleKey = "CategoryCode_GRP_Name",
        descriptionKey = "CategoryCode_GRP_Description",
        settingsKey = SettingsKey.GRP,
    ),

    MILX(
        code = "MILX",
        iconKey = "category_milx",
        titleKey = "CategoryCode_MILX_Name",
        descriptionKey = "CategoryCode_MILX_Description",
        settingsKey = SettingsKey.MILX,
    ),

    MISC(
        code = "MISC",
        iconKey = "category_misc",
        titleKey = "CategoryCode_MISC_Name",
        descriptionKey = "CategoryCode_MISC_Description",
        settingsKey = null,
    ),
    ;

    companion object {
        fun fromCode(code: String?) = code?.let { entries.firstOrNull { it.code == code } } ?: MISC
    }
}
