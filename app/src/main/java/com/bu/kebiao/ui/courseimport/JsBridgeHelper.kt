package com.bu.kebiao.ui.courseimport

object JsBridgeHelper {

    val promiseBridgeJs = """
(function() {
    if (window._bridgePromiseInitialized) return;
    window._bridgePromiseInitialized = true;
    
    var pendingPromises = {};
    var promiseCounter = 0;
    
    window._resolveAndroidPromise = function(promiseId, result) {
        var p = pendingPromises[promiseId];
        if (p) {
            p.resolve(result);
            delete pendingPromises[promiseId];
        }
    };
    
    window._rejectAndroidPromise = function(promiseId, error) {
        var p = pendingPromises[promiseId];
        if (p) {
            p.reject(new Error(error));
            delete pendingPromises[promiseId];
        }
    };
    
    function callAsync(method, args) {
        return new Promise(function(resolve, reject) {
            promiseCounter++;
            var promiseId = 'p_' + promiseCounter;
            pendingPromises[promiseId] = { resolve: resolve, reject: reject };
            var allArgs = Array.prototype.slice.call(args);
            allArgs.push(promiseId);
            AndroidBridge[method].apply(AndroidBridge, allArgs);
        });
    }
    
    window.AndroidBridgePromise = {
        showAlert: function(title, content, confirmText) {
            return callAsync('showAlert', [title, content, confirmText]);
        },
        saveImportedCourses: function(jsonString) {
            return callAsync('saveImportedCourses', [jsonString]);
        },
        savePresetTimeSlots: function(jsonString) {
            return callAsync('savePresetTimeSlots', [jsonString]);
        },
        saveCourseConfig: function(jsonString) {
            return callAsync('saveCourseConfig', [jsonString]);
        }
    };
})();
"""

    fun buildAdapterScript(jsContent: String): String {
        return promiseBridgeJs + "\n" + jsContent
    }
}
