package az.fleetra.mobile

import android.app.Application
import az.fleetra.mobile.data.TokenStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class FleetraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
        // Hydrate the in-memory token cache from disk before any screen can
        // possibly make an API call — see TokenStore for why this matters.
        MainScope().launch { TokenStore.hydrate() }
    }
}
