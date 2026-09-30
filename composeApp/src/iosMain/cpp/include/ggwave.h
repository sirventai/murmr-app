#ifndef GGWAVE_H
#define GGWAVE_H

#ifdef GGWAVE_SHARED
#    ifdef _WIN32
#        ifdef GGWAVE_BUILD
#            define GGWAVE_API __declspec(dllexport)
#        else
#            define GGWAVE_API __declspec(dllimport)
#        endif
#    else
#        define GGWAVE_API __attribute__ ((visibility ("default")))
#    endif
#else
#    define GGWAVE_API
#endif

#if defined(ARDUINO_UNO)
#define GGWAVE_CONFIG_FEW_PROTOCOLS
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define GGWAVE_MAX_INSTANCES 4

    typedef enum {
        GGWAVE_SAMPLE_FORMAT_UNDEFINED,
        GGWAVE_SAMPLE_FORMAT_U8,
        GGWAVE_SAMPLE_FORMAT_I8,
        GGWAVE_SAMPLE_FORMAT_U16,
        GGWAVE_SAMPLE_FORMAT_I16,
        GGWAVE_SAMPLE_FORMAT_F32,
    } ggwave_SampleFormat;

    typedef enum {
#ifndef GGWAVE_CONFIG_FEW_PROTOCOLS
        GGWAVE_PROTOCOL_AUDIBLE_NORMAL,
        GGWAVE_PROTOCOL_AUDIBLE_FAST,
        GGWAVE_PROTOCOL_AUDIBLE_FASTEST,
        GGWAVE_PROTOCOL_ULTRASOUND_NORMAL,
        GGWAVE_PROTOCOL_ULTRASOUND_FAST,
        GGWAVE_PROTOCOL_ULTRASOUND_FASTEST,
#endif
        GGWAVE_PROTOCOL_DT_NORMAL,
        GGWAVE_PROTOCOL_DT_FAST,
        GGWAVE_PROTOCOL_DT_FASTEST,
        GGWAVE_PROTOCOL_MT_NORMAL,
        GGWAVE_PROTOCOL_MT_FAST,
        GGWAVE_PROTOCOL_MT_FASTEST,

#ifndef GGWAVE_CONFIG_FEW_PROTOCOLS
        GGWAVE_PROTOCOL_CUSTOM_0,
        GGWAVE_PROTOCOL_CUSTOM_1,
        GGWAVE_PROTOCOL_CUSTOM_2,
        GGWAVE_PROTOCOL_CUSTOM_3,
        GGWAVE_PROTOCOL_CUSTOM_4,
        GGWAVE_PROTOCOL_CUSTOM_5,
        GGWAVE_PROTOCOL_CUSTOM_6,
        GGWAVE_PROTOCOL_CUSTOM_7,
        GGWAVE_PROTOCOL_CUSTOM_8,
        GGWAVE_PROTOCOL_CUSTOM_9,

#endif
        GGWAVE_PROTOCOL_COUNT,
    } ggwave_ProtocolId;

    typedef enum {
        GGWAVE_FILTER_HANN,
        GGWAVE_FILTER_HAMMING,
        GGWAVE_FILTER_FIRST_ORDER_HIGH_PASS,
    } ggwave_Filter;

    enum {
        GGWAVE_OPERATING_MODE_RX            = 1 << 1,
        GGWAVE_OPERATING_MODE_TX            = 1 << 2,
        GGWAVE_OPERATING_MODE_RX_AND_TX     = (GGWAVE_OPERATING_MODE_RX |
                                               GGWAVE_OPERATING_MODE_TX),
        GGWAVE_OPERATING_MODE_TX_ONLY_TONES = 1 << 3,
        GGWAVE_OPERATING_MODE_USE_DSS       = 1 << 4,
    };

    typedef struct {
        int                 payloadLength;
        float               sampleRateInp;
        float               sampleRateOut;
        float               sampleRate;
        int                 samplesPerFrame;
        float               soundMarkerThreshold;
        ggwave_SampleFormat sampleFormatInp;
        ggwave_SampleFormat sampleFormatOut;
        int                 operatingMode;
    } ggwave_Parameters;

    typedef int ggwave_Instance;

    GGWAVE_API void ggwave_setLogFile(void * fptr);

    GGWAVE_API ggwave_Parameters ggwave_getDefaultParameters(void);

    GGWAVE_API ggwave_Instance ggwave_init(ggwave_Parameters parameters);

    GGWAVE_API void ggwave_free(ggwave_Instance instance);

    GGWAVE_API int ggwave_encode(
            ggwave_Instance instance,
            const void * payloadBuffer,
            int payloadSize,
            ggwave_ProtocolId protocolId,
            int volume,
            void * waveformBuffer,
            int query);

    GGWAVE_API int ggwave_decode(
            ggwave_Instance instance,
            const void * waveformBuffer,
            int waveformSize,
            void * payloadBuffer);

    GGWAVE_API int ggwave_ndecode(
            ggwave_Instance instance,
            const void * waveformBuffer,
            int waveformSize,
            void * payloadBuffer,
            int payloadSize);

    GGWAVE_API void ggwave_rxToggleProtocol(
            ggwave_ProtocolId protocolId,
            int state);

    GGWAVE_API void ggwave_txToggleProtocol(
            ggwave_ProtocolId protocolId,
            int state);

    GGWAVE_API void ggwave_rxProtocolSetFreqStart(
            ggwave_ProtocolId protocolId,
            int freqStart);

    GGWAVE_API void ggwave_txProtocolSetFreqStart(
            ggwave_ProtocolId protocolId,
            int freqStart);

    GGWAVE_API int ggwave_rxDurationFrames(
            ggwave_Instance instance);

#ifdef __cplusplus
}

template <typename T>
struct ggvector {
private:
    T * m_data;
    int m_size;

public:
    using value_type = T;

    ggvector() : m_data(nullptr), m_size(0) {}
    ggvector(T * data, int size) : m_data(data), m_size(size) {}

    ggvector(const ggvector<T> & other) = default;

    ggvector & operator=(const ggvector &) = delete;
    ggvector & operator=(ggvector &&) = delete;

    T & operator[](int i) { return m_data[i]; }
    const T & operator[](int i) const { return m_data[i]; }

    int size() const { return m_size; }
    T * data() const { return m_data; }

    T * begin() const { return m_data; }
    T * end() const { return m_data + m_size; }

    void assign(const ggvector & other);
    void copy(const ggvector & other);

    void zero();
    void zero(int n);
};

template <typename T>
struct ggmatrix {
private:
    T * m_data;
    int m_size0;
    int m_size1;

public:
    using value_type = T;

    ggmatrix() : m_data(nullptr), m_size0(0), m_size1(0) {}
    ggmatrix(T * data, int size0, int size1) : m_data(data), m_size0(size0), m_size1(size1) {}

    ggvector<T> operator[](int i) {
        return ggvector<T>(m_data + i*m_size1, m_size1);
    }

    int size() const { return m_size0; }

    void zero();
};

#include <stdint.h>
#include <stdio.h>

#ifdef ARDUINO
#if defined(ARDUINO_ARCH_AVR) || defined(ARDUINO_ARCH_SAMD) || defined(ARDUINO_ARDUINO_NANO33BLE) || defined(ARDUINO_ARCH_MBED_RP2040) || defined(ARDUINO_ARCH_RP2040)
#include <avr/pgmspace.h>
#else
#include <pgmspace.h>
#endif
#endif

class GGWave {
public:
    static constexpr auto kSampleRateMin               = 1000.0f;
    static constexpr auto kSampleRateMax               = 96000.0f;
    static constexpr auto kDefaultSampleRate           = 48000.0f;
    static constexpr auto kDefaultSamplesPerFrame      = 1024;
    static constexpr auto kDefaultVolume               = 10;
    static constexpr auto kDefaultSoundMarkerThreshold = 3.0f;
    static constexpr auto kDefaultMarkerFrames         = 16;
    static constexpr auto kDefaultEncodedDataOffset    = 3;
    static constexpr auto kMaxSamplesPerFrame          = 1024;
    static constexpr auto kMaxDataSize                 = 256;
    static constexpr auto kMaxLengthVariable           = 140;
    static constexpr auto kMaxLengthFixed              = 64;
    static constexpr auto kMaxSpectrumHistory          = 4;
    static constexpr auto kMaxRecordedFrames           = 2048;

    using Parameters    = ggwave_Parameters;
    using SampleFormat  = ggwave_SampleFormat;
    using ProtocolId    = ggwave_ProtocolId;
    using TxProtocolId  = ggwave_ProtocolId;
    using RxProtocolId  = ggwave_ProtocolId;
    using OperatingMode = int;

    struct Protocol {
        const char * name;

        int16_t freqStart;
        int8_t  framesPerTx;
        int8_t  bytesPerTx;
        int8_t  extra;

        bool enabled;

        int nTones() const { return (2*bytesPerTx)/extra; }
        int nDataBitsPerTx() const { return 8*bytesPerTx; }
        int txDuration_ms(int samplesPerFrame, float sampleRate) const {
            return framesPerTx*((1000.0f*samplesPerFrame)/sampleRate);
        }
    };

    using TxProtocol = Protocol;
    using RxProtocol = Protocol;

    struct Protocols;

    using TxProtocols = Protocols;
    using RxProtocols = Protocols;

    struct Protocols {
        Protocol data[GGWAVE_PROTOCOL_COUNT];

        int size() const {
            return GGWAVE_PROTOCOL_COUNT;
        }

        bool empty() const {
            return size() == 0;
        }

        Protocol & operator[](ProtocolId id) {
            return data[id];
        }

        Protocol & operator[](int id) {
            return data[id];
        }

        const Protocol & operator[](ProtocolId id) const {
            return data[id];
        }

        const Protocol & operator[](int id) const {
            return data[id];
        }

        void enableAll();
        void disableAll();

        void toggle(ProtocolId id, bool state);
        void only(ProtocolId id);

        static Protocols & kDefault() {
            static Protocols protocols;

            static bool initialized = false;
            if (initialized == false) {
                for (int i = 0; i < GGWAVE_PROTOCOL_COUNT; ++i) {
                    protocols.data[i].name = nullptr;
                    protocols.data[i].enabled = false;
                }

#if defined(ARDUINO_AVR_UNO)

#define GGWAVE_PSTR PSTR
#else
#define GGWAVE_PSTR(str) (str)
#endif

#ifndef GGWAVE_CONFIG_FEW_PROTOCOLS
                protocols.data[GGWAVE_PROTOCOL_AUDIBLE_NORMAL]     = { GGWAVE_PSTR("Normal"),       40,  9, 3, 1, true, };
                protocols.data[GGWAVE_PROTOCOL_AUDIBLE_FAST]       = { GGWAVE_PSTR("Fast"),         40,  6, 3, 1, true, };
                protocols.data[GGWAVE_PROTOCOL_AUDIBLE_FASTEST]    = { GGWAVE_PSTR("Fastest"),      40,  3, 3, 1, true, };
                protocols.data[GGWAVE_PROTOCOL_ULTRASOUND_NORMAL]  = { GGWAVE_PSTR("[U] Normal"),   320, 9, 3, 1, true, };
                protocols.data[GGWAVE_PROTOCOL_ULTRASOUND_FAST]    = { GGWAVE_PSTR("[U] Fast"),     320, 6, 3, 1, true, };
                protocols.data[GGWAVE_PROTOCOL_ULTRASOUND_FASTEST] = { GGWAVE_PSTR("[U] Fastest"),  320, 3, 3, 1, true, };
#endif
                protocols.data[GGWAVE_PROTOCOL_DT_NORMAL]          = { GGWAVE_PSTR("[DT] Normal"),  24,  9, 1, 1, true, };
                protocols.data[GGWAVE_PROTOCOL_DT_FAST]            = { GGWAVE_PSTR("[DT] Fast"),    24,  6, 1, 1, true, };
                protocols.data[GGWAVE_PROTOCOL_DT_FASTEST]         = { GGWAVE_PSTR("[DT] Fastest"), 24,  3, 1, 1, true, };
                protocols.data[GGWAVE_PROTOCOL_MT_NORMAL]          = { GGWAVE_PSTR("[MT] Normal"),  24,  9, 1, 2, true, };
                protocols.data[GGWAVE_PROTOCOL_MT_FAST]            = { GGWAVE_PSTR("[MT] Fast"),    24,  6, 1, 2, true, };
                protocols.data[GGWAVE_PROTOCOL_MT_FASTEST]         = { GGWAVE_PSTR("[MT] Fastest"), 24,  3, 1, 2, true, };

#undef GGWAVE_PSTR
                initialized = true;
            }

            return protocols;
        }

        static TxProtocols & tx();
        static RxProtocols & rx();
    };

    using Tone = int8_t;

    using Tones = ggvector<Tone>;

    using Amplitude    = ggvector<float>;
    using AmplitudeArr = ggmatrix<float>;
    using AmplitudeI16 = ggvector<int16_t>;
    using Spectrum     = ggvector<float>;
    using RecordedData = ggvector<float>;
    using TxRxData     = ggvector<uint8_t>;

    GGWave() = default;

    GGWave(const Parameters & parameters);

    ~GGWave();

    bool prepare(const Parameters & parameters, bool allocate = true);

    static void setLogFile(FILE * fptr);

    static const Parameters & getDefaultParameters();

    bool init(const char * text, TxProtocolId protocolId, const int volume = kDefaultVolume);
    bool init(int dataSize, const char * dataBuffer, TxProtocolId protocolId, const int volume = kDefaultVolume);

    uint32_t encodeSize_bytes() const;

    uint32_t encodeSize_samples() const;

    uint32_t encode();

    bool decode(const void * data, uint32_t nBytes);

    bool isDSSEnabled() const;

    int samplesPerFrame() const;
    int sampleSizeInp()   const;
    int sampleSizeOut()   const;

    float hzPerSample()   const;
    float sampleRateInp() const;
    float sampleRateOut() const;
    SampleFormat sampleFormatInp() const;
    SampleFormat sampleFormatOut() const;

    int heapSize() const;

    const void * txWaveform() const;

    const Tones txTones() const;

    bool txHasData() const;

    bool txTakeAmplitudeI16(AmplitudeI16 & dst);

    const TxProtocols & txProtocols() const;

    bool rxReceiving() const;
    bool rxAnalyzing() const;

    int rxSamplesNeeded()       const;
    int rxFramesToRecord()      const;
    int rxFramesLeftToRecord()  const;
    int rxFramesToAnalyze()     const;
    int rxFramesLeftToAnalyze() const;
    int rxDurationFrames()      const;

    bool rxStopReceiving();

    RxProtocols & rxProtocols();

    int                  rxDataLength() const;
    const TxRxData &     rxData()       const;
    const RxProtocol &   rxProtocol()   const;
    const RxProtocolId & rxProtocolId() const;
    const Spectrum &     rxSpectrum()   const;
    const Amplitude &    rxAmplitude()  const;

    int rxTakeData(TxRxData & dst);

    bool rxTakeSpectrum(Spectrum & dst);
    bool rxTakeAmplitude(Amplitude & dst);

    bool computeFFTR(const float * src, float * dst, int N);

    static int computeFFTR(const float * src, float * dst, int N, int * wi, float * wf);

    static int filter(ggwave_Filter filter, float * waveform, int N, float p0, float p1, float * w);

    class Resampler {
    public:

        static const int kWidth = 64;

        Resampler();

        bool alloc(void * p, int & n);

        void reset();

        int nSamplesTotal() const { return m_state.nSamplesTotal; }

        int resample(
                float factor,
                int nSamples,
                const float * samplesInp,
                float * samplesOut);

    private:
        float getData(int j) const;
        void newData(float data);
        void makeSinc();
        double sinc(double x) const;

        static const int kDelaySize = 140;

        static const int kSamplesPerZeroCrossing = 32;

        ggvector<float> m_sincTable;
        ggvector<float> m_delayBuffer;
        ggvector<float> m_edgeSamples;
        ggvector<float> m_samplesInp;

        struct State {
            int nSamplesTotal = 0;
            int timeInt       = 0;
            int timeLast      = 0;
            double timeNow    = 0.0;
        };

        State m_state;
    };

private:
    bool alloc(void * p, int & n);

    void decode_fixed();
    void decode_variable();

    int maxFramesPerTx(const Protocols & protocols, bool excludeMT) const;
    int minBytesPerTx(const Protocols & protocols) const;
    int maxBytesPerTx(const Protocols & protocols) const;
    int maxTonesPerTx(const Protocols & protocols) const;
    int minFreqStart(const Protocols & protocols) const;

    double bitFreq(const Protocol & p, int bit) const;

    float        m_sampleRateInp        = -1.0f;
    float        m_sampleRateOut        = -1.0f;
    float        m_sampleRate           = -1.0f;
    int          m_samplesPerFrame      = -1;
    float        m_isamplesPerFrame     = -1.0f;
    int          m_sampleSizeInp        = -1;
    int          m_sampleSizeOut        = -1;
    SampleFormat m_sampleFormatInp      = GGWAVE_SAMPLE_FORMAT_UNDEFINED;
    SampleFormat m_sampleFormatOut      = GGWAVE_SAMPLE_FORMAT_UNDEFINED;

    float        m_hzPerSample          = -1.0f;
    float        m_ihzPerSample         = -1.0f;

    int          m_freqDelta_bin        = -1;
    float        m_freqDelta_hz         = -1.0f;

    int          m_nBitsInMarker        = -1;
    int          m_nMarkerFrames        = -1;
    int          m_encodedDataOffset    = -1;

    float        m_soundMarkerThreshold = -1.0f;

    bool         m_isFixedPayloadLength = false;
    int          m_payloadLength        = -1;

    bool         m_isRxEnabled          = false;
    bool         m_isTxEnabled          = false;
    bool         m_needResampling       = false;
    bool         m_txOnlyTones          = false;
    bool         m_isDSSEnabled         = false;

    TxRxData m_dataEncoded;
    TxRxData m_workRSLength;
    TxRxData m_workRSData;

    struct Rx {
        bool receiving = false;
        bool analyzing = false;

        int nMarkersSuccess     = 0;
        int markerFreqStart     = 0;
        int recvDuration_frames = 0;
        int minFreqStart        = 0;

        int framesLeftToAnalyze = 0;
        int framesLeftToRecord  = 0;
        int framesToAnalyze     = 0;
        int framesToRecord      = 0;
        int samplesNeeded       = 0;

        ggvector<float> fftOut;
        ggvector<int>   fftWorkI;
        ggvector<float> fftWorkF;

        bool hasNewRxData    = false;
        bool hasNewSpectrum  = false;
        bool hasNewAmplitude = false;

        Spectrum  spectrum;
        Amplitude amplitude;
        Amplitude amplitudeResampled;
        TxRxData  amplitudeTmp;

        int dataLength = 0;

        TxRxData     data;
        RxProtocol   protocol;
        RxProtocolId protocolId;
        RxProtocols  protocols;

        int historyId = 0;

        Amplitude    amplitudeAverage;
        AmplitudeArr amplitudeHistory;
        RecordedData amplitudeRecorded;

        int historyIdFixed = 0;

        ggmatrix<uint8_t> spectrumHistoryFixed;
        ggvector<uint8_t> detectedBins;
        ggvector<uint8_t> detectedTones;
    } m_rx;

    struct Tx {
        bool hasData = false;

        float sendVolume = 0.1f;

        int dataLength = 0;
        int lastAmplitudeSize = 0;

        ggvector<bool> dataBits;
        ggvector<double> phaseOffsets;

        AmplitudeArr bit1Amplitude;
        AmplitudeArr bit0Amplitude;

        TxRxData    data;
        TxProtocol  protocol;
        TxProtocols protocols;

        Amplitude    output;
        Amplitude    outputResampled;
        TxRxData     outputTmp;
        AmplitudeI16 outputI16;

        int nTones = 0;
        Tones tones;
    } m_tx;

    mutable Resampler m_resampler;

    void * m_heap  = nullptr;
    int m_heapSize = 0;
};

#endif

#endif
