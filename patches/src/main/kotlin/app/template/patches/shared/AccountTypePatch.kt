package app.template.patches.shared

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

/**
 * Android lets only the first-installed app that declares an account type add accounts
 * of that type, and only apps signed with its certificate may share it. Yahoo apps all
 * share "com.yahoo.mobile.client.share.account", so with Yahoo Mail (or any other Yahoo
 * app) installed, a re-signed Yahoo Finance is refused by AccountManager and crashes
 * after sign-in. Give it its own type. Phoenix reads the type from this string and
 * accepts any type that starts with an allowlisted prefix, so the suffix passes its check.
 */
internal val accountTypePatch = resourcePatch {
    execute {
        document("res/values/strings.xml").use { doc ->
            val strings = doc.getElementsByTagName("string")
            val accountType = (0 until strings.length)
                .map { strings.item(it) as Element }
                .single { it.getAttribute("name") == "account_type" }
            accountType.textContent = "com.yahoo.mobile.client.share.account.morphe"
        }
    }
}
