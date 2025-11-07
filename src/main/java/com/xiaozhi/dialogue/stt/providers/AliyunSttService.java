package com.xiaozhi.dialogue.stt.providers;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerParam;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerRealtime;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.xiaozhi.dialogue.stt.SttService;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.utils.AudioUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

public class AliyunSttService implements SttService {
    private static final Logger logger = LoggerFactory.getLogger(AliyunSttService.class);
    private static final String PROVIDER_NAME = "aliyun";

    private final String apiKey;
    private final String model;
    public AliyunSttService(SysConfig config) {
        this.apiKey = config.getApiKey();
        this.model = config.getConfigName();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public String recognition(byte[] audioData) {
        // 单次识别暂未实现，可以根据需要添加
        logger.warn("阿里云单次识别未实现，请使用流式识别");
        return null;
    }

    @Override
    public String streamRecognition(Sinks.Many<byte[]> audioSink) {
        try {
            if (model.toLowerCase().contains("gummy")) {
                return streamRecognitionGummy(audioSink);
            } else {
                // paraformer 逻辑
                String actualModel = model;
                // 兼容以前的数据，如果不包含已知模型类型，则使用默认模型
                if (!model.toLowerCase().contains("paraformer") 
                    && !model.toLowerCase().contains("fun-asr")) {
                    actualModel = "paraformer-realtime-v2";
                    logger.info("未识别的模型类型: {}，使用默认模型: {}", model, actualModel);
                }
                return streamRecognitionParaformer(audioSink, actualModel);
            }
        } catch (Exception e) {
            logger.error("使用{}模型语音识别失败：", model, e);
            return "";
        }
    }

    /**
     * Paraformer 模型的流式识别
     */
    private String streamRecognitionParaformer(Sinks.Many<byte[]> audioSink, String modelName) {
        var recognizer = new Recognition();

        // 创建识别参数
        var param = RecognitionParam.builder()
                .model(modelName)
                .format("pcm")
                .sampleRate(AudioUtils.SAMPLE_RATE) // 使用16000Hz采样率
                .apiKey(apiKey)
                .build();

        // 使用 Reactor 执行流式识别
        var recognition = Flux.<String>create(sink -> {
            try {
                recognizer.streamCall(param, Flowable.create(emitter -> {
                            audioSink.asFlux().subscribe(
                                    chunk -> emitter.onNext(ByteBuffer.wrap(chunk)),
                                    emitter::onError,
                                    emitter::onComplete
                            );
                        }, BackpressureStrategy.BUFFER))
                        .timeout(90, TimeUnit.SECONDS)
                        .subscribe(result -> {
                                    if (result.isSentenceEnd()) {
                                        logger.info("语音识别结果({}): {}", modelName, result.getSentence().getText());
                                        sink.next(result.getSentence().getText());
                                    }
                                },
                                Throwable::printStackTrace,
                                sink::complete
                        );
            } catch (Exception e) {
                sink.error(e);
                logger.info("使用{}模型语音识别失败：", modelName, e);
            }
        });

        return recognition.reduce(new StringBuffer(), StringBuffer::append)
                .blockOptional()
                .map(StringBuffer::toString)
                .orElse("");
    }

    /**
     * Gummy 模型的流式识别（支持实时翻译）
     */
    private String streamRecognitionGummy(Sinks.Many<byte[]> audioSink) {
        StringBuilder result = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean hasError = new AtomicBoolean(false);
        
        // 初始化请求参数
        var param = TranslationRecognizerParam.builder()
                .apiKey(apiKey)
                .model(model)
                .format("pcm")
                .sampleRate(AudioUtils.SAMPLE_RATE)
                .transcriptionEnabled(true)
                .sourceLanguage("auto")
                .build();
        
        // 初始化回调接口
        ResultCallback<TranslationRecognizerResult> callback = 
                new ResultCallback<TranslationRecognizerResult>() {
                    @Override
                    public void onEvent(TranslationRecognizerResult recognizerResult) {
                        try {
                            
                            // 处理识别结果
                            if (recognizerResult.getTranscriptionResult() != null) {
                                if (recognizerResult.isSentenceEnd()) {
                                    String text = recognizerResult.getTranscriptionResult().getText();
                                    logger.info("语音识别结果({}): {}", model, text);
                                    synchronized (result) {
                                        result.append(text);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("处理识别结果时发生错误", e);
                        }
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        logger.error("语音识别错误({}): {}", model, e.getMessage(), e);
                        hasError.set(true);
                        latch.countDown();
                    }
                };
        
        // 初始化流式识别服务
        TranslationRecognizerRealtime translator = new TranslationRecognizerRealtime();
        
        try {
            // 启动流式语音识别
            translator.call(param, callback);
            
            // 订阅音频流并发送数据
            audioSink.asFlux().subscribe(
                    audioChunk -> {
                        try {
                            ByteBuffer buffer = ByteBuffer.wrap(audioChunk);
                            translator.sendAudioFrame(buffer);
                        } catch (Exception e) {
                            logger.error("发送音频数据时发生错误", e);
                        }
                    },
                    error -> {
                        logger.error("音频流错误", error);
                        translator.stop();
                        latch.countDown();
                    },
                    () -> {
                        translator.stop();
                    }
            );
            
            // 等待识别完成，最多90秒
            boolean completed = latch.await(90, TimeUnit.SECONDS);
            
            if (!completed) {
                logger.warn("语音识别超时({})", model);
            }
            
        } catch (Exception e) {
            logger.error("流式识别过程中发生错误({})", model, e);
            hasError.set(true);
        } finally {
            // 关闭 websocket 连接
            try {
                translator.getDuplexApi().close(1000, "bye");
            } catch (Exception e) {
                logger.error("关闭连接时发生错误", e);
            }
        }
        
        if (hasError.get()) {
            return "";
        }
        
        return result.toString();
    }
}