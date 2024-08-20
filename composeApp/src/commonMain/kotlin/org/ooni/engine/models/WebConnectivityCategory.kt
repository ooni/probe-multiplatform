package org.ooni.engine.models

import ooniprobe.composeapp.generated.resources.CategoryCode_ALDR_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_ALDR_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_ANON_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_ANON_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_COMM_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_COMM_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_COMT_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_COMT_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_CTRL_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_CTRL_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_CULTR_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_CULTR_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_DATE_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_DATE_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_ECON_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_ECON_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_ENV_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_ENV_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_FILE_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_FILE_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_GAME_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_GAME_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_GMB_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_GMB_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_GOVT_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_GOVT_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_GRP_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_GRP_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_HACK_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_HACK_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_HATE_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_HATE_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_HOST_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_HOST_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_HUMR_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_HUMR_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_IGO_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_IGO_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_LGBT_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_LGBT_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_MILX_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_MILX_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_MISC_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_MISC_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_MMED_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_MMED_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_NEWS_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_NEWS_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_POLR_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_POLR_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_PORN_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_PORN_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_PROV_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_PROV_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_PUBH_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_PUBH_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_REL_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_REL_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_SRCH_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_SRCH_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_XED_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_XED_Name
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.category_aldr
import ooniprobe.composeapp.generated.resources.category_anon
import ooniprobe.composeapp.generated.resources.category_comm
import ooniprobe.composeapp.generated.resources.category_comt
import ooniprobe.composeapp.generated.resources.category_ctrl
import ooniprobe.composeapp.generated.resources.category_cultr
import ooniprobe.composeapp.generated.resources.category_date
import ooniprobe.composeapp.generated.resources.category_econ
import ooniprobe.composeapp.generated.resources.category_env
import ooniprobe.composeapp.generated.resources.category_file
import ooniprobe.composeapp.generated.resources.category_game
import ooniprobe.composeapp.generated.resources.category_gmb
import ooniprobe.composeapp.generated.resources.category_govt
import ooniprobe.composeapp.generated.resources.category_grp
import ooniprobe.composeapp.generated.resources.category_hack
import ooniprobe.composeapp.generated.resources.category_hate
import ooniprobe.composeapp.generated.resources.category_host
import ooniprobe.composeapp.generated.resources.category_humr
import ooniprobe.composeapp.generated.resources.category_igo
import ooniprobe.composeapp.generated.resources.category_lgbt
import ooniprobe.composeapp.generated.resources.category_milx
import ooniprobe.composeapp.generated.resources.category_misc
import ooniprobe.composeapp.generated.resources.category_mmed
import ooniprobe.composeapp.generated.resources.category_news
import ooniprobe.composeapp.generated.resources.category_polr
import ooniprobe.composeapp.generated.resources.category_porn
import ooniprobe.composeapp.generated.resources.category_prov
import ooniprobe.composeapp.generated.resources.category_pubh
import ooniprobe.composeapp.generated.resources.category_rel
import ooniprobe.composeapp.generated.resources.category_srch
import ooniprobe.composeapp.generated.resources.category_xed
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.ooni.probe.data.models.SettingsKey

enum class WebConnectivityCategory(
    val code: String,
    val icon: DrawableResource,
    val title: StringResource,
    val description: StringResource,
    val settingsKey: SettingsKey?,
) {
    ANON(
        code = "ANON",
        icon = Res.drawable.category_anon,
        title = Res.string.CategoryCode_ANON_Name,
        description = Res.string.CategoryCode_ANON_Description,
        settingsKey = SettingsKey.ANON,
    ),

    COMT(
        code = "COMT",
        icon = Res.drawable.category_comt,
        title = Res.string.CategoryCode_COMT_Name,
        description = Res.string.CategoryCode_COMT_Description,
        settingsKey = SettingsKey.COMT,
    ),

    CTRL(
        code = "CTRL",
        icon = Res.drawable.category_ctrl,
        title = Res.string.CategoryCode_CTRL_Name,
        description = Res.string.CategoryCode_CTRL_Description,
        settingsKey = SettingsKey.CTRL,
    ),

    CULTR(
        code = "CULTR",
        icon = Res.drawable.category_cultr,
        title = Res.string.CategoryCode_CULTR_Name,
        description = Res.string.CategoryCode_CULTR_Description,
        settingsKey = SettingsKey.CULTR,
    ),

    ALDR(
        code = "ALDR",
        icon = Res.drawable.category_aldr,
        title = Res.string.CategoryCode_ALDR_Name,
        description = Res.string.CategoryCode_ALDR_Description,
        settingsKey = SettingsKey.ALDR,
    ),

    COMM(
        code = "COMM",
        icon = Res.drawable.category_comm,
        title = Res.string.CategoryCode_COMM_Name,
        description = Res.string.CategoryCode_COMM_Description,
        settingsKey = SettingsKey.COMM,
    ),

    ECON(
        code = "ECON",
        icon = Res.drawable.category_econ,
        title = Res.string.CategoryCode_ECON_Name,
        description = Res.string.CategoryCode_ECON_Description,
        settingsKey = SettingsKey.ECON,
    ),

    ENV(
        code = "ENV",
        icon = Res.drawable.category_env,
        title = Res.string.CategoryCode_ENV_Name,
        description = Res.string.CategoryCode_ENV_Description,
        settingsKey = SettingsKey.ENV,
    ),

    FILE(
        code = "FILE",
        icon = Res.drawable.category_file,
        title = Res.string.CategoryCode_FILE_Name,
        description = Res.string.CategoryCode_FILE_Description,
        settingsKey = SettingsKey.FILE,
    ),

    GMB(
        code = "GMB",
        icon = Res.drawable.category_gmb,
        title = Res.string.CategoryCode_GMB_Name,
        description = Res.string.CategoryCode_GMB_Description,
        settingsKey = SettingsKey.GMB,
    ),

    GAME(
        code = "GAME",
        icon = Res.drawable.category_game,
        title = Res.string.CategoryCode_GAME_Name,
        description = Res.string.CategoryCode_GAME_Description,
        settingsKey = SettingsKey.GAME,
    ),

    GOVT(
        code = "GOVT",
        icon = Res.drawable.category_govt,
        title = Res.string.CategoryCode_GOVT_Name,
        description = Res.string.CategoryCode_GOVT_Description,
        settingsKey = SettingsKey.GOVT,
    ),

    HACK(
        code = "HACK",
        icon = Res.drawable.category_hack,
        title = Res.string.CategoryCode_HACK_Name,
        description = Res.string.CategoryCode_HACK_Description,
        settingsKey = SettingsKey.HACK,
    ),

    HATE(
        code = "HATE",
        icon = Res.drawable.category_hate,
        title = Res.string.CategoryCode_HATE_Name,
        description = Res.string.CategoryCode_HATE_Description,
        settingsKey = SettingsKey.HATE,
    ),

    HOST(
        code = "HOST",
        icon = Res.drawable.category_host,
        title = Res.string.CategoryCode_HOST_Name,
        description = Res.string.CategoryCode_HOST_Description,
        settingsKey = SettingsKey.HOST,
    ),

    HUMR(
        code = "HUMR",
        icon = Res.drawable.category_humr,
        title = Res.string.CategoryCode_HUMR_Name,
        description = Res.string.CategoryCode_HUMR_Description,
        settingsKey = SettingsKey.HUMR,
    ),

    IGO(
        code = "IGO",
        icon = Res.drawable.category_igo,
        title = Res.string.CategoryCode_IGO_Name,
        description = Res.string.CategoryCode_IGO_Description,
        settingsKey = SettingsKey.IGO,
    ),

    LGBT(
        code = "LGBT",
        icon = Res.drawable.category_lgbt,
        title = Res.string.CategoryCode_LGBT_Name,
        description = Res.string.CategoryCode_LGBT_Description,
        settingsKey = SettingsKey.LGBT,
    ),

    MMED(
        code = "MMED",
        icon = Res.drawable.category_mmed,
        title = Res.string.CategoryCode_MMED_Name,
        description = Res.string.CategoryCode_MMED_Description,
        settingsKey = SettingsKey.MMED,
    ),

    NEWS(
        code = "NEWS",
        icon = Res.drawable.category_news,
        title = Res.string.CategoryCode_NEWS_Name,
        description = Res.string.CategoryCode_NEWS_Description,
        settingsKey = SettingsKey.NEWS,
    ),

    DATE(
        code = "DATE",
        icon = Res.drawable.category_date,
        title = Res.string.CategoryCode_DATE_Name,
        description = Res.string.CategoryCode_DATE_Description,
        settingsKey = SettingsKey.DATE,
    ),

    POLR(
        code = "POLR",
        icon = Res.drawable.category_polr,
        title = Res.string.CategoryCode_POLR_Name,
        description = Res.string.CategoryCode_POLR_Description,
        settingsKey = SettingsKey.POLR,
    ),

    PORN(
        code = "PORN",
        icon = Res.drawable.category_porn,
        title = Res.string.CategoryCode_PORN_Name,
        description = Res.string.CategoryCode_PORN_Description,
        settingsKey = SettingsKey.PORN,
    ),

    PROV(
        code = "PROV",
        icon = Res.drawable.category_prov,
        title = Res.string.CategoryCode_PROV_Name,
        description = Res.string.CategoryCode_PROV_Description,
        settingsKey = SettingsKey.PROV,
    ),

    PUBH(
        code = "PUBH",
        icon = Res.drawable.category_pubh,
        title = Res.string.CategoryCode_PUBH_Name,
        description = Res.string.CategoryCode_PUBH_Description,
        settingsKey = SettingsKey.PUBH,
    ),

    REL(
        code = "REL",
        icon = Res.drawable.category_rel,
        title = Res.string.CategoryCode_REL_Name,
        description = Res.string.CategoryCode_REL_Description,
        settingsKey = SettingsKey.REL,
    ),

    SRCH(
        code = "SRCH",
        icon = Res.drawable.category_srch,
        title = Res.string.CategoryCode_SRCH_Name,
        description = Res.string.CategoryCode_SRCH_Description,
        settingsKey = SettingsKey.SRCH,
    ),

    XED(
        code = "XED",
        icon = Res.drawable.category_xed,
        title = Res.string.CategoryCode_XED_Name,
        description = Res.string.CategoryCode_XED_Description,
        settingsKey = SettingsKey.XED,
    ),

    GRP(
        code = "GRP",
        icon = Res.drawable.category_grp,
        title = Res.string.CategoryCode_GRP_Name,
        description = Res.string.CategoryCode_GRP_Description,
        settingsKey = SettingsKey.GRP,
    ),

    MILX(
        code = "MILX",
        icon = Res.drawable.category_milx,
        title = Res.string.CategoryCode_MILX_Name,
        description = Res.string.CategoryCode_MILX_Description,
        settingsKey = SettingsKey.MILX,
    ),

    MISC(
        code = "MISC",
        icon = Res.drawable.category_misc,
        title = Res.string.CategoryCode_MISC_Name,
        description = Res.string.CategoryCode_MISC_Description,
        settingsKey = null,
    ),
    ;

    companion object {
        fun fromCode(code: String?) = code?.let { entries.firstOrNull { it.code == code } } ?: MISC
    }
}
