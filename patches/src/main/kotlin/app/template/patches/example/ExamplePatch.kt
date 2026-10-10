package app.template.patches.example

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.template.patches.shared.Constants.COMPATIBILITY_YAHOO_FINANCE
import app.template.patches.shared.accountTypePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val ADS_FREE_FEATURE =
    "Lcom/yahoo/mobile/client/android/finance/subscription/v2/model/SubscriptionFeature\$AdsFree;"

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes stream, pencil and markets ads from Yahoo Finance at the " +
        "source so they are never fetched, loaded or shown.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_YAHOO_FINANCE)
    dependsOn(accountTypePatch)

    execute {
        // 1. Stream/feed ads (Home, News, QSP).
        // The feed ad provider returns the next ad unit to insert into the feed.
        // Return a fresh empty ad unit at the very top of the method: the body that
        // fetches a real ad becomes dead code, so nothing is ever requested.
        // The empty unit's class name is obfuscated, so take it from the method's own
        // AdsFree early return, which is the first new-instance in the body.
        // The method is `declared-synchronized`, but DEX takes the lock with an
        // explicit monitor-enter; returning before it holds no monitor.
        val getNextAd = GetNextAdFingerprint.method
        val emptyAdUnit = getNextAd.implementation!!.instructions
            .first { it.opcode == Opcode.NEW_INSTANCE }
            .let { (it as ReferenceInstruction).reference as TypeReference }.type
        getNextAd.addInstructions(
            0,
            """
                new-instance v0, $emptyAdUnit
                invoke-direct {v0}, $emptyAdUnit-><init>()V
                return-object v0
            """,
        )

        // 2. Home top "pencil" banner: force canAdBeShown() to return false.
        PencilAdCanBeShownFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        // 3. Markets top-center ad: force canAdBeShown() to return false.
        MarketsAdCanBeShownFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        // 3b. Markets screen ad module: report ads as hidden (ad-free user path),
        // which stops the fetch and removes the empty placeholder container.
        MarketsAreAdsHiddenFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )

        // 3c. Newer ad surfaces (Home daily brief, news, Inception slots) only ask
        // the subscription manager whether AdsFree is accessible. Answer HAS_ACCESS
        // for AdsFree alone so each takes the app's own ad-free path; every other
        // feature falls through to the original lookup. v0 is a scratch local here.
        FeatureAccessibilityFingerprint.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    instance-of v0, p1, $ADS_FREE_FEATURE
                    if-eqz v0, :original
                    sget-object v0, $returnType->HAS_ACCESS:$returnType
                    return-object v0
                """,
                ExternalLabel("original", getInstruction(0)),
            )
        }

        // 4. Quote page top banner: return null from buildRowViewModel so no row
        // (and no ad fetch) is ever produced.
        TopCenterAdBuildRowFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )

        // 5. Quote page pencil / native ads (Summary, Analysis, Financials):
        // same null-row removal.
        PencilAdBuildRowFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )
    }
}
