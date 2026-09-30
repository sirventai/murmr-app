package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy
import uoc.edu.psirvent.murmr.util.Logger

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberAvatarPicker(onResult: (ByteArray?) -> Unit): () -> Unit {
    val callbackRef = rememberUpdatedState(onResult)

    val delegateHolder = remember { arrayOfNulls<NSObject>(1) }
    return remember {
        {
            val rootVC = UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .firstOrNull()
                ?.windows
                ?.firstOrNull { (it as? platform.UIKit.UIWindow)?.isKeyWindow() == true }
                ?.let { (it as platform.UIKit.UIWindow).rootViewController }
                ?: UIApplication.sharedApplication.keyWindow?.rootViewController

            if (rootVC == null) {
                Logger.w("AvatarPicker", "No rootViewController disponible")
                callbackRef.value(null)
                return@remember
            }

            val config = PHPickerConfiguration().apply {
                setSelectionLimit(1)
                setFilter(PHPickerFilter.imagesFilter())
            }
            val picker = PHPickerViewController(configuration = config)
            val delegate = AvatarPickerDelegate { bytes ->
                dispatch_async(dispatch_get_main_queue()) {
                    delegateHolder[0] = null
                    callbackRef.value(bytes)
                }
            }
            delegateHolder[0] = delegate
            picker.delegate = delegate
            rootVC.presentViewController(picker, animated = true, completion = null)
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class AvatarPickerDelegate(
    private val onBytes: (ByteArray?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    private val tag = "AvatarPicker"

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            Logger.i(tag, "Picker cerrado sin selección")
            onBytes(null)
            return
        }
        val provider = result.itemProvider
        val typeId = "public.image"
        if (!provider.hasItemConformingToTypeIdentifier(typeId)) {
            Logger.w(tag, "Item provider no es imagen")
            onBytes(null)
            return
        }
        provider.loadDataRepresentationForTypeIdentifier(typeId) { data: NSData?, error: NSError? ->
            when {
                error != null -> {
                    Logger.w(tag, "loadData error: ${error.localizedDescription}")
                    onBytes(null)
                }
                data == null -> {
                    Logger.w(tag, "loadData devolvió null")
                    onBytes(null)
                }
                else -> {
                    val bytes = data.toByteArray()
                    Logger.i(tag, "Imagen seleccionada size=${bytes.size}B")
                    onBytes(bytes)
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val len = length.toInt()
    if (len == 0) return ByteArray(0)
    val out = ByteArray(len)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, length)
    }
    return out
}
