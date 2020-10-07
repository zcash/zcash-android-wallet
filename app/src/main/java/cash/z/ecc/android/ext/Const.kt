package cash.z.ecc.android.ext

object Const {
    /**
     * Named objects for Dependency Injection
     */
    object Name {
        /** application data other than cryptographic keys */
        const val APP_PREFS = "const.name.app_prefs"
        const val BEFORE_SYNCHRONIZER = "const.name.before_synchronizer"
        const val SYNCHRONIZER = "const.name.synchronizer"
    }

    /**
     * App preference key names
     */
    object Pref {
        const val FIRST_USE_VIEW_TX = "const.pref.first_use_view_tx"
        const val FEEDBACK_ENABLED = "const.pref.feedback_enabled"
        const val SERVER_HOST = "const.pref.server_host"
        const val SERVER_PORT = "const.pref.server_port"
    }

    /**
     * Default values to use application-wide. Ideally, this set of values should remain very short.
     */
    object Default {
        object Server {
            // If you've forked the ECC repo, change this to your hosted lightwalletd instance
            const val HOST = "lightwalletd.electriccoin.co"//"your.hosted.lightwalletd.org"
            const val PORT = 9067
        }
    }
}
