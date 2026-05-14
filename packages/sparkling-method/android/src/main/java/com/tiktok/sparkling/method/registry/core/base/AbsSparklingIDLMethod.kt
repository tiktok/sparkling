// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.base

import android.webkit.WebView
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.IDLMethodRegistryCacheManager
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.exception.IDLMethodException
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.model.idl.IDLDynamic
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel
import com.tiktok.sparkling.method.registry.core.model.idl.getValue
import com.tiktok.sparkling.method.registry.core.model.idl.toPrimitiveOrJSON
import com.tiktok.sparkling.method.registry.core.utils.JsonUtils
import com.lynx.tasm.LynxView
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

abstract class AbsSparklingIDLMethod<INPUT, OUTPUT> : IDLBridgeMethod where INPUT : IDLMethodBaseParamModel, OUTPUT : IDLMethodBaseResultModel {
    private var bridgeContext: IBridgeContext? = null
    var contextProviderFactory: ContextProviderFactory? = null

    override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {
        this.contextProviderFactory = contextProviderFactory
    }

    override fun setBridgeContext(bridgeContext: IBridgeContext) {
        this.bridgeContext = bridgeContext
    }

    fun getSDKContext(): IBridgeContext? = this.bridgeContext

    @Deprecated("Please use [getSDKContext] ", ReplaceWith(""))
    open fun <T> provideContext(clz: Class<T>): T? = contextProviderFactory?.provideInstance(clz)

    fun getXValue(data: Any?): Any? {
        if (data is IDLDynamic) {
            return data.getValue()
        }
        return data
    }

    fun getXValue(
        map: Map<String, Any>,
        key: String,
    ): Any? {
        val value = map[key]
        if (value is IDLDynamic) {
            return value.getValue()
        }
        return value
    }

    fun toJSON(list: List<Any>): JSONArray = JsonUtils.listToJSON(list)

    fun toJSON(map: Map<String, Any>): JSONObject = JsonUtils.mapToJSON(map)

    fun toJSON(data: IDLMethodBaseModel?): JSONObject {
        if (data == null) {
            return JSONObject()
        }
        return data.toJSON()
    }

    fun onSuccess(
        callback: IDLBridgeMethod.Callback,
        data: Map<String, Any>,
        msg: String = "",
    ) {
        if (useOriginalResult) {
            if (data[IDLBridgeMethod.ORIGINAL_RESULT] != null && data[IDLBridgeMethod.ORIGINAL_RESULT] is Map<*, *>) {
                callback.invoke(data[IDLBridgeMethod.ORIGINAL_RESULT] as Map<String, Any?>)
            } else {
                callback.invoke(data)
            }
        } else {
            if (shouldUseOriginalData(getSDKContext())) {
                callback.invoke(
                    mutableMapOf<String, Any>().apply {
                        put(IDLBridgeMethod.PARAM_CODE, IDLBridgeMethod.SUCCESS)
                        put(IDLBridgeMethod.PARAM_MSG, msg)
                        putAll(data)
                    },
                )
            } else {
                callback.invoke(
                    mutableMapOf<String, Any>().apply {
                        put(IDLBridgeMethod.PARAM_CODE, IDLBridgeMethod.SUCCESS)
                        put(IDLBridgeMethod.PARAM_MSG, msg)
                        put(IDLBridgeMethod.PARAM_DATA, data)
                    },
                )
            }
        }
    }

    fun onFailure(
        callback: IDLBridgeMethod.Callback,
        code: Int,
        msg: String = "",
        data: Map<String, Any> = mutableMapOf(),
    ) {
        if (useOriginalResult) {
            if (data[IDLBridgeMethod.ORIGINAL_RESULT] != null && data[IDLBridgeMethod.ORIGINAL_RESULT] is Map<*, *>) {
                callback.invoke(data[IDLBridgeMethod.ORIGINAL_RESULT] as Map<String, Any?>)
            } else {
                callback.invoke(data)
            }
        } else {
            if (shouldUseOriginalData(getSDKContext())) {
                callback.invoke(
                    mutableMapOf<String, Any>().apply {
                        put(IDLBridgeMethod.PARAM_CODE, code)
                        put(IDLBridgeMethod.PARAM_MSG, msg)
                        putAll(data)
                    },
                )
            } else {
                callback.invoke(
                    mutableMapOf<String, Any>().apply {
                        put(IDLBridgeMethod.PARAM_CODE, code)
                        put(IDLBridgeMethod.PARAM_MSG, msg)
                        put(IDLBridgeMethod.PARAM_DATA, data)
                    },
                )
            }
        }
    }

    abstract fun handle(
        params: INPUT,
        callback: CompletionBlock<OUTPUT>,
        type: BridgePlatformType,
    )

    override fun realHandle(
        params: Map<String, Any?>,
        callback: IDLBridgeMethod.Callback,
        type: BridgePlatformType,
    ) {
        val paramModel = createParamModelProxy(params)
        if (paramModel == null) {
            onFailure(callback, IDLBridgeMethod.INVALID_PARAM, "")
            return
        }

        val completionBlock = createCompletionBlockProxy<OUTPUT>(this.javaClass.classLoader!!, callback)
        getSDKContext()?.getObject(BridgeContext::class.java)?.let { bridgeContext ->
            bridgeContext.bridgeLifeClientImp.onBridgeImplHandleStart(null, bridgeContext)
        }
        this.handle(paramModel, completionBlock, type)
        getSDKContext()?.getObject(BridgeContext::class.java)?.let { bridgeContext ->
            bridgeContext.bridgeLifeClientImp.onBridgeImplHandleEnd(null, bridgeContext)
        }
    }

    @Throws(IllegalStateException::class, IDLMethodException::class)
    private fun createParamModelProxy(dataSource: Map<String, Any?>): INPUT? {
        val clazz = getParamsClazz() ?: throw IllegalStateException("params class is null")
        return Proxy.newProxyInstance(
            clazz.classLoader,
            arrayOf(clazz),
            InvocationHandler { proxy, method, args ->
                if (method.name.equals("toJSON")) {
                    return@InvocationHandler runCatching {
                        return@runCatching JSONObject().apply {
                            dataSource
                                .mapValues {
                                    return@mapValues when (it.value) {
                                        is Long -> {
                                            it.value
                                        }

                                        is Int -> {
                                            it.value
                                        }

                                        is Double -> {
                                            it.value
                                        }

                                        is String -> {
                                            it.value
                                        }

                                        is Boolean -> {
                                            it.value
                                        }

                                        is List<*> -> {
                                            JsonUtils.toJSONArray(it.value as List<*>)
                                        }

                                        is Map<*, *> -> {
                                            JsonUtils.toJSONObject(it.value as Map<*, *>)
                                        }

                                        is IDLDynamic -> {
                                            (it.value as IDLDynamic).toPrimitiveOrJSON()
                                        }

                                        else -> {
                                            // nested class type
                                            if (it.value is IDLMethodBaseModel) {
                                                (it.value as IDLMethodBaseModel).toJSON()
                                            } else {
                                                null
                                            }
                                        }
                                    }
                                }.forEach { entry ->
                                    entry.value?.let {
                                        put(entry.key, entry.value)
                                    }
                                }
                        }
                    }.getOrDefault(JSONObject())
                }

                if (method.name.equals("toString")) {
                    return@InvocationHandler "Just need to check if the relevant fields can be obtained, " +
                        "no need to pay attention to the overall situation of the current model. " +
                        "just like this: params.xxx;" + "dataSource = $dataSource"
                }

                val propertyName = getPropertyName(method)
                return@InvocationHandler dataSource[propertyName]
            },
        ) as INPUT
    }

    private fun getPropertyName(method: Method): String {
        val pool =
            (
                getSDKContext()?.containerID?.let {
                    IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(it)
                }
            )?.BRIDGE_ANNOTATION_MAP?.get(this::class.java)
        if (pool != null) {
            val keyPath = pool.methodParamModel.methodModel[method]?.keyPath
            if (keyPath != null) {
                return keyPath
            }
        }

        val annotationModel = method.getAnnotation(IDLMethodParamField::class.java)
        val propertyName = annotationModel.keyPath
        return propertyName
    }

    private fun getParamsClazz(): Class<*>? {
        val paramClass =
            (
                getSDKContext()?.containerID?.let {
                    IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(it)
                }
            )?.BRIDGE_ANNOTATION_MAP?.get(this::class.java)?.paramClass
        if (paramClass != null) {
            return paramClass
        }
        println("idl Map->Model. no cache")
        var paramModelList =
            this.javaClass.declaredClasses.filter {
                it.getAnnotation(
                    IDLMethodParamModel::class.java,
                ) != null
            }
        if (paramModelList.isEmpty()) {
            paramModelList =
                this.javaClass.superclass.declaredClasses.filter {
                    it.getAnnotation(
                        IDLMethodParamModel::class.java,
                    ) != null
                }
            if (paramModelList.isEmpty()) {
                throw IllegalStateException("Illegal class format, no param model is defined in class")
            }
        }

        return paramModelList.firstOrNull()
    }

    fun <OUTPUT> createCompletionBlockProxy(
        classLoader: ClassLoader,
        callback: IDLBridgeMethod.Callback,
    ): CompletionBlock<OUTPUT> where OUTPUT : IDLMethodBaseResultModel =
        object : CompletionBlock<OUTPUT> {
            override fun onSuccess(
                result: OUTPUT,
                msg: String,
            ) {
                val resultMap = result.convert() ?: mutableMapOf()
                this@AbsSparklingIDLMethod.onSuccess(callback = callback, data = resultMap, msg = msg)
            }

            override fun onFailure(
                code: Int,
                msg: String,
                data: OUTPUT?,
            ) {
                val resultMap = data?.convert() ?: mutableMapOf()
                this@AbsSparklingIDLMethod.onFailure(callback, code, msg, resultMap)
            }

            override fun onRawSuccess(data: OUTPUT?) {
                val resultMap = data?.convert() ?: mutableMapOf()
                callback.invoke(resultMap)
            }
        }

    protected fun getHybridUrl(type: BridgePlatformType): String? =
        if (type == BridgePlatformType.LYNX) {
            (getSDKContext()?.view as? LynxView)?.templateUrl
        } else {
            (getSDKContext()?.view as? WebView)?.url
        }
}
