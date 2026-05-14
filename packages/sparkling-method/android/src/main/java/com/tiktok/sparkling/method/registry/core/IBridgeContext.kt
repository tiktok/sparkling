// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import android.app.Activity
import android.content.Context
import android.view.View
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.core.model.context.IContextProvider
import com.tiktok.sparkling.method.registry.core.model.context.ContextHolder
import com.tiktok.sparkling.method.registry.core.model.context.WeakContextHolder
import com.tiktok.sparkling.method.registry.core.utils.IDLMethodHelper
import org.json.JSONObject
import java.lang.ref.WeakReference

interface IBridgeContext {
    val bridge: SparklingBridge?
    val view: View?
    val ownerActivity: Activity?
    val containerID: String?
    val context: Context?

    fun sendEvent(
        name: String,
        obj: JSONObject,
    )

    fun <T> getObject(clazz: Class<T>): T?
}

interface JSEventDelegate {
    fun sendJSEvent(
        eventName: String,
        params: JSONObject?,
    )
}

class BridgeContextWrapper : IBridgeContext {
    private val providers: MutableMap<Class<*>, IContextProvider<*>> = mutableMapOf()
    private var _view: WeakReference<View?>? = null
    private var _activity: WeakReference<Activity?>? = null
    private var _containerID: String? = null
    private var _jsEvent: JSEventDelegate? = null
    private var __bridge: WeakReference<SparklingBridge>? = null

    /**
     * register object to JSBSDKContext, you can use getSDKContext().getObject(clazz) in JSB method to get it.
     * there is two-ways to register object to JSBSDKContext: registerObject & registerWeakObject
     * 1. registerObject register an object to JSBSDKContext. It uses strong references to ensure that objects exist when used.
     * 2. registerWeakObject register a weakObject to JSBSDKContext.
     *    It uses weak references to store the object, so when the object is released, maybe you will get a null when you use it.
     * 3. registerWeakObject has higher priority than registerObject,
     *    so if you register the same class with object and weakObject, you only can get the weakObject.
     */
    fun <T> registerWeakObject(
        clazz: Class<T>,
        t: T?,
    ) {
        providers[clazz] = WeakContextHolder(t)
    }

    /**
     * register object to JSBSDKContext, you can use getSDKContext().getObject(clazz) in JSB method to get it.
     * there is two-ways to register object to JSBSDKContext: registerObject & registerWeakObject
     * 1. registerObject register an object to JSBSDKContext. It uses strong references to ensure that objects exist when used.
     * 2. registerWeakObject register a weakObject to JSBSDKContext.
     *    It uses weak references to store the object, so when the object is released, maybe you will get a null when you use it.
     * 3. registerWeakObject has higher priority than registerObject,
     *    so if you register the same class with object and weakObject, you only can get the weakObject.
     */
    fun <T> registerObject(
        clazz: Class<T>,
        t: T?,
    ) {
        providers[clazz] = ContextHolder(t)
    }

    /**
     * register object to JSBSDKContext, you can use getSDKContext().getObject(clazz) in JSB method to get it.
     * there is two-ways to register object to JSBSDKContext: registerObject & registerWeakObject
     * 1. registerObject register an object to JSBSDKContext. It uses strong references to ensure that objects exist when used.
     * 2. registerWeakObject register a weakObject to JSBSDKContext.
     *    It uses weak references to store the object, so when the object is released, maybe you will get a null when you use it.
     * 3. registerWeakObject has higher priority than registerObject,
     *    so if you register the same class with object and weakObject, you only can get the weakObject.
     */
    fun registerWeakObjects(map: Map<Class<*>, WeakContextHolder<*>>) {
        providers.putAll(map)
    }

    /**
     * register object to JSBSDKContext, you can use getSDKContext().getObject(clazz) in JSB method to get it.
     * there is two-ways to register object to JSBSDKContext: registerObject & registerWeakObject
     * 1. registerObject register an object to JSBSDKContext. It uses strong references to ensure that objects exist when used.
     * 2. registerWeakObject register a weakObject to JSBSDKContext.
     *    It uses weak references to store the object, so when the object is released, maybe you will get a null when you use it.
     * 3. registerWeakObject has higher priority than registerObject,
     *    so if you register the same class with object and weakObject, you only can get the weakObject.
     */
    fun registerObjects(map: Map<Class<*>, ContextHolder<*>>) {
        providers.putAll(map)
    }

    fun setJSEventDelegate(delegate: JSEventDelegate) {
        if (_jsEvent == null) {
            _jsEvent = delegate
        }
    }

    fun setView(view: View?) {
        if (_view == null) {
            _view = WeakReference(view)
        }
    }

    fun setContainerID(containerID: String?) {
        _containerID = containerID
    }

    fun setOwnerActivity(activity: Activity?) {
        if (_activity?.get() == null) {
            _activity = WeakReference(activity)
        }
    }

    fun setBridge(bridge: SparklingBridge) {
        if (__bridge?.get() == null) {
            __bridge = WeakReference(bridge)
        }
    }

    override val bridge: SparklingBridge?
        get() = __bridge?.get()

    override val view: View?
        get() = _view?.get() ?: getObject(View::class.java)
    override val ownerActivity: Activity?
        get() = WeakReference(IDLMethodHelper.getActivity(context)).get()
    override val context: Context?
        get() = _view?.get()?.context ?: getObject(Context::class.java)

    override fun sendEvent(
        name: String,
        obj: JSONObject,
    ) {
        _jsEvent?.sendJSEvent(name, obj)
    }

    override val containerID: String?
        get() = _containerID

    override fun <T> getObject(clazz: Class<T>): T? = providers[clazz]?.provideInstance() as T?
}
