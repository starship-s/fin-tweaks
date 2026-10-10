package app.template.patches.example

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_YAHOO_FINANCE
import app.template.patches.shared.accountTypePatch

@Suppress("unused")
val removeNagsPatch = bytecodePatch(
    name = "Remove nags",
    description = "Suppresses the 'Rate this app' prompt and the 'subscribe to " +
        "Premium' upsell notification in the activity feed.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_YAHOO_FINANCE)
    dependsOn(accountTypePatch)

    execute {
        // "Rate this app" -> never ask.
        AppRateAskFingerprint.methodOrNull?.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        // Feed Premium-upsell notification -> return null (no card).
        UpsellNotificationFetchFingerprint.methodOrNull?.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )
    }
}
