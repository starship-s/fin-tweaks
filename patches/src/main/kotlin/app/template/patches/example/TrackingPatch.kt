package app.template.patches.example

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_YAHOO_FINANCE

@Suppress("unused")
val blockTrackingPatch = bytecodePatch(
    name = "Block tracking & telemetry",
    description = "Stops comScore, AppsFlyer, Embrace and Yahoo (YAnalytics) from " +
        "collecting or sending tracking and telemetry data.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_YAHOO_FINANCE)

    execute {
        // Each target returns void; a return-void at the top no-ops it. methodOrNull
        // keeps the patch resilient if one SDK is absent in a future app version.
        val noop = "return-void"

        ComscoreStartFingerprint.methodOrNull?.addInstructions(0, noop)
        // AppsFlyer init now reports an InitializeResult enum; report it as disabled.
        AppsFlyerInitFingerprint.methodOrNull?.let {
            val result = it.returnType
            it.addInstructions(
                0,
                if (result == "V") noop else "sget-object v0, $result->FEATURE_DISABLED:$result\nreturn-object v0",
            )
        }
        EmbraceStartFingerprint.methodOrNull?.addInstructions(0, noop)
        EmbraceStartWithFrameworkFingerprint.methodOrNull?.addInstructions(0, noop)
        YAnalyticsLogEventFingerprint.methodOrNull?.addInstructions(0, noop)
    }
}
