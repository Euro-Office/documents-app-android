package app.editors.manager.ui.fragments.main.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.editors.manager.R
import app.editors.manager.viewModels.main.SpellingDictionary
import lib.compose.ui.views.AppListItem
import lib.compose.ui.views.PlaceholderView
import lib.toolkit.base.managers.utils.StringUtils


/** The spelling dictionaries the editors downloaded; a tap removes one ([onDictionaryClick]). */
@Composable
fun AppSettingsDictionariesScreen(dictionaries: List<SpellingDictionary>, onDictionaryClick: (SpellingDictionary) -> Unit = {}) {
    if (dictionaries.isEmpty()) {
        PlaceholderView(
            image = lib.toolkit.base.R.drawable.placeholder_empty_folder,
            title = stringResource(id = R.string.placeholder_no_dictionaries),
            subtitle = stringResource(id = R.string.placeholder_no_dictionaries_desc)
        )
    } else {
        val context = LocalContext.current
        val removeLabel = stringResource(id = R.string.settings_dictionaries_remove)
        LazyColumn {
            items(items = dictionaries, key = { it.name }) { dictionary ->
                AppListItem(
                    modifier = Modifier.animateItem(),
                    title = dictionary.language,
                    subtitle = StringUtils.getFormattedSize(context, dictionary.size),
                    singleLine = false,
                    dividerVisible = false,
                    endContent = { Icon(painter = painterResource(id = R.drawable.ic_trash), contentDescription = null) },
                    onClickLabel = removeLabel,
                    onClick = { onDictionaryClick(dictionary) }
                )
            }
        }
    }
}
