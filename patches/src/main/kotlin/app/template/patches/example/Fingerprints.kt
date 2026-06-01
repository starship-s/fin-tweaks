package app.template.patches.example

import app.morphe.patcher.Fingerprint

// The Yahoo Finance ad layer is NOT obfuscated, so we match each method by its
// exact defining class + method name (via the `custom` predicate), plus return
// type and parameters. This is far more robust than instruction fingerprints and
// survives app updates as long as Yahoo keeps these class/method names.

// AdFetcher.getNextAd(int) -> YFAdUnit : the provider of stream/feed ads
// (Home, News, QSP). Returning an EmptyAdUnit means no ad is ever fetched.
object GetNextAdFingerprint : Fingerprint(
    returnType = "Lcom/yahoo/mobile/client/android/finance/ads/model/YFAdUnit;",
    parameters = listOf("I"),
    custom = { method, classDef ->
        classDef.type == "Lcom/yahoo/mobile/client/android/finance/ads/AdFetcher;" &&
            method.name == "getNextAd"
    },
)

// PencilAdDelegate.canAdBeShown() -> boolean : the Home top "pencil" banner gate.
object PencilAdCanBeShownFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == "Lcom/yahoo/mobile/client/android/finance/home/ad/PencilAdDelegate;" &&
            method.name == "canAdBeShown"
    },
)

// MarketsTopCenterAdDelegate.canAdBeShown() -> boolean : the Markets top ad gate.
object MarketsAdCanBeShownFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == "Lcom/yahoo/mobile/client/android/finance/markets/ad/MarketsTopCenterAdDelegate;" &&
            method.name == "canAdBeShown"
    },
)

// Quote (symbol detail) page top banner. TopCenterAdModule.buildRowViewModel(...)
// builds the banner row; it already returns null as its "no ad" path (the isHidden
// branch), so returning null removes the banner and the ad is never fetched.
object TopCenterAdBuildRowFingerprint : Fingerprint(
    returnType = "Lcom/yahoo/mobile/client/android/finance/core/app/model/RowViewModel;",
    parameters = listOf(
        "Landroid/content/Context;",
        "Lcom/yahoo/mobile/client/android/finance/quote/model/QspViewData;",
    ),
    custom = { method, classDef ->
        classDef.type == "Lcom/yahoo/mobile/client/android/finance/quote/model/TopCenterAdModule;" &&
            method.name == "buildRowViewModel"
    },
)

// Quote page pencil / native ad (Summary, Analysis and Financials tabs).
// BasePencilAdModule.buildRowViewModel(...) builds the pencil ad row with the same
// null semantics; PencilAdSummary/Analysis/Financials all inherit this method.
object PencilAdBuildRowFingerprint : Fingerprint(
    returnType = "Lcom/yahoo/mobile/client/android/finance/core/app/model/RowViewModel;",
    parameters = listOf(
        "Landroid/content/Context;",
        "Lcom/yahoo/mobile/client/android/finance/quote/model/QspViewData;",
    ),
    custom = { method, classDef ->
        classDef.type == "Lcom/yahoo/mobile/client/android/finance/quote/model/BasePencilAdModule;" &&
            method.name == "buildRowViewModel"
    },
)
