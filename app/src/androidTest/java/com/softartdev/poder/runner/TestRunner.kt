package com.softartdev.poder.runner

import com.softartdev.poder.PoderApp
import android.app.Application
import android.content.Context
import io.appflate.restmock.android.RESTMockTestRunner

/**
 * Created by ravindra on 4/2/17.
 */
class TestRunner : RESTMockTestRunner() {

    @Throws(InstantiationException::class, IllegalAccessException::class, ClassNotFoundException::class)
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(cl, PoderApp::class.java.name, context)
    }

}
