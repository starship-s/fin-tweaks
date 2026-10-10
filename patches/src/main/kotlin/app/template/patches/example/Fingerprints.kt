package app.template.patches.example

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

// Since 26.1x Yahoo Finance obfuscates member names (and some class names, e.g.
// AdFetcher), so hooks match on what survives R8: the defining class or package,
// the method shape, and references to unobfuscated SDK/enum types and strings.

private const val FINANCE = "Lcom/yahoo/mobile/client/android/finance/"
private const val ADS_FREE = FINANCE + "subscription/v2/model/SubscriptionFeature\$AdsFree;"
private const val SM_PLACEMENT_CONFIG = "Lcom/oath/mobile/ads/sponsoredmoments/config/SMAdPlacementConfig;"
private const val FIREBASE_ANALYTICS = "Lcom/google/firebase/analytics/FirebaseAnalytics;"

private fun Method.references(type: String) =
    implementation?.instructions?.any {
        (it as? ReferenceInstruction)?.reference?.toString()?.contains(type) == true
    } == true

// Stream/feed ad provider (Home, News, QSP): (int) -> ad unit, whose first branch
// returns the empty ad unit for AdsFree subscribers. Formerly AdFetcher.getNextAd.
object GetNextAdFingerprint : Fingerprint(
    parameters = listOf("I"),
    custom = { method, classDef ->
        classDef.type.startsWith(FINANCE + "ads/") &&
            method.returnType.startsWith(FINANCE + "ads/model/") &&
            method.references(ADS_FREE)
    },
)

// Home top "pencil" banner gate. Formerly PencilAdDelegate.canAdBeShown().
object PencilAdCanBeShownFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == FINANCE + "home/ad/PencilAdDelegate;" &&
            method.references(SM_PLACEMENT_CONFIG)
    },
)

// Markets top ad gate. Formerly MarketsTopCenterAdDelegate.canAdBeShown().
object MarketsAdCanBeShownFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == FINANCE + "markets/ad/MarketsTopCenterAdDelegate;" &&
            method.references(SM_PLACEMENT_CONFIG)
    },
)

// Markets screen ad module AdsFree check. Forcing it true takes the app's own
// "ad-free user" path: no fetch and no empty placeholder container.
// Formerly MarketsTopCenterAdModule.areAdsHidden().
object MarketsAreAdsHiddenFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == FINANCE + "markets/ad/MarketsTopCenterAdModule;" &&
            method.references(ADS_FREE)
    },
)

// Subscription feature -> accessibility lookup. Every AdsFree check in the app
// (feed, Home daily brief, news, Inception slots, video) resolves through it.
object FeatureAccessibilityFingerprint : Fingerprint(
    returnType = FINANCE + "subscription/SubscriptionManager\$FeatureAccessibility;",
    parameters = listOf(FINANCE + "subscription/v2/model/SubscriptionFeature;"),
    custom = { method, classDef ->
        classDef.type == FINANCE + "subscription/SubscriptionManagerHilt;" &&
            method.references(ADS_FREE)
    },
)

// Quote page modules build their row from (Context, view data); null is the
// module's own "no ad" result. Formerly buildRowViewModel(Context, QspViewData).
private fun Method.isBuildRow() =
    parameterTypes.size == 2 &&
        parameterTypes[0] == "Landroid/content/Context;" &&
        returnType.startsWith(FINANCE + "core/app/model/")

// Quote (symbol detail) page top banner.
object TopCenterAdBuildRowFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == FINANCE + "quote/model/TopCenterAdModule;" && method.isBuildRow()
    },
)

// Quote page pencil / native ad (Summary, Analysis, Financials all inherit it).
object PencilAdBuildRowFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == FINANCE + "quote/model/BasePencilAdModule;" && method.isBuildRow()
    },
)

// ---- Tracking / telemetry (separate "Block tracking" patch) ----

// comScore audience measurement: no-op SDK startup.
object ComscoreStartFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    custom = { method, classDef ->
        classDef.type == "Lcom/comscore/Analytics;" && method.name == "start"
    },
)

// AppsFlyer install/attribution tracking: the app's SDK init routine, found by its
// log string. Returns AppsFlyerManager$InitializeResult since 26.1x.
object AppsFlyerInitFingerprint : Fingerprint(
    strings = listOf("[AppsFlyer] SDK initialization start"),
    custom = { _, classDef ->
        classDef.type == FINANCE + "appsflyer/AppsFlyerManager;"
    },
)

// Embrace session/perf telemetry: no-op both start() overloads (isStarted stays
// false, so all later Embrace calls become no-ops).
object EmbraceStartFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    custom = { method, classDef ->
        classDef.type == "Lio/embrace/android/embracesdk/Embrace;" && method.name == "start"
    },
)

object EmbraceStartWithFrameworkFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Lio/embrace/android/embracesdk/AppFramework;"),
    custom = { method, classDef ->
        classDef.type == "Lio/embrace/android/embracesdk/Embrace;" && method.name == "start"
    },
)

// Yahoo's YAnalytics -> Firebase bridge: drop every logged event. Formerly
// YAnalyticsLoggerImpl.logEvent; now a default interface method that every
// implementation reaches through invoke-super.
object YAnalyticsLogEventFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lcom/yahoo/mobile/yanalytics/common/event/b;"),
    custom = { method, classDef ->
        classDef.type.startsWith(FINANCE + "analytics/") && method.references(FIREBASE_ANALYTICS)
    },
)

// ---- Nags (separate "Remove nags" patch) ----

// "Rate this app" prompt decision. Formerly AppRateManager.askForRateOrSentiment.
object AppRateAskFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(
        "Landroid/content/Context;",
        "J",
        FINANCE + "analytics/data/TrackingData;",
    ),
    custom = { _, classDef ->
        classDef.type == FINANCE + "feedback/rating/AppRateManager;"
    },
)

// Feed "subscribe to Premium" upsell: suspend invoke() returns a nullable
// UpsellNotificationParams. Returning null suppresses the card.
object UpsellNotificationFetchFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    custom = { method, classDef ->
        classDef.type == FINANCE + "activityfeed/usecase/FetchUpsellNotificationUseCase;" &&
            method.parameterTypes.size == 1 &&
            method.parameterTypes[0].startsWith("Lkotlin/coroutines/")
    },
)
