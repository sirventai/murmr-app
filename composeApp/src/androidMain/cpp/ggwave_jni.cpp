
#include <jni.h>
#include <vector>
#include <cstring>
#include "ggwave.h"

static constexpr float kSampleRate = 48000.0f;

extern "C" {

JNIEXPORT jint JNICALL
Java_uoc_edu_psirvent_murmr_audio_ggwave_GgwaveCodec_nativeInit(
        JNIEnv* env, jobject , jint protocolId) {
    ggwave_Parameters p = ggwave_getDefaultParameters();
    p.payloadLength       = -1;
    p.sampleRateInp       = kSampleRate;
    p.sampleRateOut       = kSampleRate;
    p.sampleRate          = kSampleRate;
    p.sampleFormatInp     = GGWAVE_SAMPLE_FORMAT_I16;
    p.sampleFormatOut     = GGWAVE_SAMPLE_FORMAT_I16;
    p.operatingMode       = GGWAVE_OPERATING_MODE_RX_AND_TX;
    ggwave_Instance inst  = ggwave_init(p);
    if (inst < 0) return -1;

    for (int i = 0; i < GGWAVE_PROTOCOL_COUNT; ++i) {
        ggwave_rxToggleProtocol(static_cast<ggwave_ProtocolId>(i), 0);
    }
    ggwave_rxToggleProtocol(static_cast<ggwave_ProtocolId>(protocolId), 1);
    return inst;
}

JNIEXPORT jshortArray JNICALL
Java_uoc_edu_psirvent_murmr_audio_ggwave_GgwaveCodec_nativeEncode(
        JNIEnv* env, jobject , jint handle, jbyteArray payload,
        jint protocolId, jint volume) {
    if (handle < 0) return nullptr;
    jsize payloadLen = env->GetArrayLength(payload);
    jbyte* payloadBytes = env->GetByteArrayElements(payload, nullptr);

    int n = ggwave_encode(handle, payloadBytes, payloadLen,
                          static_cast<ggwave_ProtocolId>(protocolId),
                          volume, nullptr, 1);
    if (n <= 0) {
        env->ReleaseByteArrayElements(payload, payloadBytes, JNI_ABORT);
        return nullptr;
    }

    int samples = n / 2;
    std::vector<int16_t> waveform(samples);
    int produced = ggwave_encode(handle, payloadBytes, payloadLen,
                                 static_cast<ggwave_ProtocolId>(protocolId),
                                 volume, waveform.data(), 0);
    env->ReleaseByteArrayElements(payload, payloadBytes, JNI_ABORT);
    if (produced <= 0) return nullptr;

    int producedSamples = produced / 2;
    jshortArray out = env->NewShortArray(producedSamples);
    if (!out) return nullptr;
    env->SetShortArrayRegion(out, 0, producedSamples,
                             reinterpret_cast<jshort*>(waveform.data()));
    return out;
}

JNIEXPORT jbyteArray JNICALL
Java_uoc_edu_psirvent_murmr_audio_ggwave_GgwaveCodec_nativeDecode(
        JNIEnv* env, jobject , jint handle, jshortArray samples) {
    if (handle < 0) return nullptr;
    jsize n = env->GetArrayLength(samples);
    jshort* sampleData = env->GetShortArrayElements(samples, nullptr);

    constexpr int kMaxPayload = 256;
    char payload[kMaxPayload];
    int decoded = ggwave_ndecode(handle, sampleData, n * sizeof(int16_t),
                                 payload, kMaxPayload);
    env->ReleaseShortArrayElements(samples, sampleData, JNI_ABORT);
    if (decoded <= 0) return nullptr;

    jbyteArray out = env->NewByteArray(decoded);
    if (!out) return nullptr;
    env->SetByteArrayRegion(out, 0, decoded,
                            reinterpret_cast<const jbyte*>(payload));
    return out;
}

JNIEXPORT void JNICALL
Java_uoc_edu_psirvent_murmr_audio_ggwave_GgwaveCodec_nativeClose(
        JNIEnv* env, jobject , jint handle) {
    if (handle >= 0) ggwave_free(handle);
}

}
