package org.ooni.engine.models

import ooniprobe.composeapp.generated.resources.Res
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

val WebConnectivityCategory.iconRes: DrawableResource
    get() = when (iconKey) {
        "category_anon" -> Res.drawable.category_anon
        "category_comt" -> Res.drawable.category_comt
        "category_ctrl" -> Res.drawable.category_ctrl
        "category_cultr" -> Res.drawable.category_cultr
        "category_aldr" -> Res.drawable.category_aldr
        "category_comm" -> Res.drawable.category_comm
        "category_econ" -> Res.drawable.category_econ
        "category_env" -> Res.drawable.category_env
        "category_file" -> Res.drawable.category_file
        "category_gmb" -> Res.drawable.category_gmb
        "category_game" -> Res.drawable.category_game
        "category_govt" -> Res.drawable.category_govt
        "category_hack" -> Res.drawable.category_hack
        "category_hate" -> Res.drawable.category_hate
        "category_host" -> Res.drawable.category_host
        "category_humr" -> Res.drawable.category_humr
        "category_igo" -> Res.drawable.category_igo
        "category_lgbt" -> Res.drawable.category_lgbt
        "category_mmed" -> Res.drawable.category_mmed
        "category_news" -> Res.drawable.category_news
        "category_date" -> Res.drawable.category_date
        "category_polr" -> Res.drawable.category_polr
        "category_porn" -> Res.drawable.category_porn
        "category_prov" -> Res.drawable.category_prov
        "category_pubh" -> Res.drawable.category_pubh
        "category_rel" -> Res.drawable.category_rel
        "category_srch" -> Res.drawable.category_srch
        "category_xed" -> Res.drawable.category_xed
        "category_grp" -> Res.drawable.category_grp
        "category_milx" -> Res.drawable.category_milx
        "category_misc" -> Res.drawable.category_misc
        else -> Res.drawable.category_misc
    }

val WebConnectivityCategory.titleRes: StringResource
    get() = when (titleKey) {
        "CategoryCode_ANON_Name" -> Res.string.CategoryCode_ANON_Name
        "CategoryCode_COMT_Name" -> Res.string.CategoryCode_COMT_Name
        "CategoryCode_CTRL_Name" -> Res.string.CategoryCode_CTRL_Name
        "CategoryCode_CULTR_Name" -> Res.string.CategoryCode_CULTR_Name
        "CategoryCode_ALDR_Name" -> Res.string.CategoryCode_ALDR_Name
        "CategoryCode_COMM_Name" -> Res.string.CategoryCode_COMM_Name
        "CategoryCode_ECON_Name" -> Res.string.CategoryCode_ECON_Name
        "CategoryCode_ENV_Name" -> Res.string.CategoryCode_ENV_Name
        "CategoryCode_FILE_Name" -> Res.string.CategoryCode_FILE_Name
        "CategoryCode_GMB_Name" -> Res.string.CategoryCode_GMB_Name
        "CategoryCode_GAME_Name" -> Res.string.CategoryCode_GAME_Name
        "CategoryCode_GOVT_Name" -> Res.string.CategoryCode_GOVT_Name
        "CategoryCode_HACK_Name" -> Res.string.CategoryCode_HACK_Name
        "CategoryCode_HATE_Name" -> Res.string.CategoryCode_HATE_Name
        "CategoryCode_HOST_Name" -> Res.string.CategoryCode_HOST_Name
        "CategoryCode_HUMR_Name" -> Res.string.CategoryCode_HUMR_Name
        "CategoryCode_IGO_Name" -> Res.string.CategoryCode_IGO_Name
        "CategoryCode_LGBT_Name" -> Res.string.CategoryCode_LGBT_Name
        "CategoryCode_MMED_Name" -> Res.string.CategoryCode_MMED_Name
        "CategoryCode_NEWS_Name" -> Res.string.CategoryCode_NEWS_Name
        "CategoryCode_DATE_Name" -> Res.string.CategoryCode_DATE_Name
        "CategoryCode_POLR_Name" -> Res.string.CategoryCode_POLR_Name
        "CategoryCode_PORN_Name" -> Res.string.CategoryCode_PORN_Name
        "CategoryCode_PROV_Name" -> Res.string.CategoryCode_PROV_Name
        "CategoryCode_PUBH_Name" -> Res.string.CategoryCode_PUBH_Name
        "CategoryCode_REL_Name" -> Res.string.CategoryCode_REL_Name
        "CategoryCode_SRCH_Name" -> Res.string.CategoryCode_SRCH_Name
        "CategoryCode_XED_Name" -> Res.string.CategoryCode_XED_Name
        "CategoryCode_GRP_Name" -> Res.string.CategoryCode_GRP_Name
        "CategoryCode_MILX_Name" -> Res.string.CategoryCode_MILX_Name
        "CategoryCode_MISC_Name" -> Res.string.CategoryCode_MISC_Name
        else -> Res.string.CategoryCode_MISC_Name
    }

val WebConnectivityCategory.descriptionRes: StringResource
    get() = when (descriptionKey) {
        "CategoryCode_ANON_Description" -> Res.string.CategoryCode_ANON_Description
        "CategoryCode_COMT_Description" -> Res.string.CategoryCode_COMT_Description
        "CategoryCode_CTRL_Description" -> Res.string.CategoryCode_CTRL_Description
        "CategoryCode_CULTR_Description" -> Res.string.CategoryCode_CULTR_Description
        "CategoryCode_ALDR_Description" -> Res.string.CategoryCode_ALDR_Description
        "CategoryCode_COMM_Description" -> Res.string.CategoryCode_COMM_Description
        "CategoryCode_ECON_Description" -> Res.string.CategoryCode_ECON_Description
        "CategoryCode_ENV_Description" -> Res.string.CategoryCode_ENV_Description
        "CategoryCode_FILE_Description" -> Res.string.CategoryCode_FILE_Description
        "CategoryCode_GMB_Description" -> Res.string.CategoryCode_GMB_Description
        "CategoryCode_GAME_Description" -> Res.string.CategoryCode_GAME_Description
        "CategoryCode_GOVT_Description" -> Res.string.CategoryCode_GOVT_Description
        "CategoryCode_HACK_Description" -> Res.string.CategoryCode_HACK_Description
        "CategoryCode_HATE_Description" -> Res.string.CategoryCode_HATE_Description
        "CategoryCode_HOST_Description" -> Res.string.CategoryCode_HOST_Description
        "CategoryCode_HUMR_Description" -> Res.string.CategoryCode_HUMR_Description
        "CategoryCode_IGO_Description" -> Res.string.CategoryCode_IGO_Description
        "CategoryCode_LGBT_Description" -> Res.string.CategoryCode_LGBT_Description
        "CategoryCode_MMED_Description" -> Res.string.CategoryCode_MMED_Description
        "CategoryCode_NEWS_Description" -> Res.string.CategoryCode_NEWS_Description
        "CategoryCode_DATE_Description" -> Res.string.CategoryCode_DATE_Description
        "CategoryCode_POLR_Description" -> Res.string.CategoryCode_POLR_Description
        "CategoryCode_PORN_Description" -> Res.string.CategoryCode_PORN_Description
        "CategoryCode_PROV_Description" -> Res.string.CategoryCode_PROV_Description
        "CategoryCode_PUBH_Description" -> Res.string.CategoryCode_PUBH_Description
        "CategoryCode_REL_Description" -> Res.string.CategoryCode_REL_Description
        "CategoryCode_SRCH_Description" -> Res.string.CategoryCode_SRCH_Description
        "CategoryCode_XED_Description" -> Res.string.CategoryCode_XED_Description
        "CategoryCode_GRP_Description" -> Res.string.CategoryCode_GRP_Description
        "CategoryCode_MILX_Description" -> Res.string.CategoryCode_MILX_Description
        "CategoryCode_MISC_Description" -> Res.string.CategoryCode_MISC_Description
        else -> Res.string.CategoryCode_MISC_Description
    }
