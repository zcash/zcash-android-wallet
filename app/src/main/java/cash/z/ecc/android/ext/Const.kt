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
    }
}
