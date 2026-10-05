package com.fraud.decisionservice.ml;

import ai.onnxruntime.*;
import com.fraud.decisionservice.config.MlThresholds;
import com.fraud.decisionservice.config.RulesLoader;
import com.fraud.decisionservice.dto.FeatureDetails;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Map;

@Service
public class OnnxScoringService {

    private static final Logger log = LoggerFactory.getLogger(OnnxScoringService.class);

    private final ResourceLoader resourceLoader;
    private final String modelPath;
    private final RulesLoader rulesLoader;

    private OrtEnvironment env;
    private OrtSession session;

    public OnnxScoringService(ResourceLoader resourceLoader,
                              @Value("${fraud.model.path:classpath:model.onnx}") String modelPath,
                              RulesLoader rulesLoader) {
        this.resourceLoader = resourceLoader;
        this.modelPath = modelPath;
        this.rulesLoader = rulesLoader;
    }

    @PostConstruct
    public void init() {
        try {
            log.info("Loading ONNX model from: {}", modelPath);
            Resource resource = resourceLoader.getResource(modelPath);
            if (!resource.exists()) {
                throw new IllegalStateException("ONNX model file not found at " + modelPath);
            }

            byte[] modelBytes;
            try (InputStream is = resource.getInputStream()) {
                modelBytes = is.readAllBytes();
            }

            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            this.session = env.createSession(modelBytes, options);

            log.info("ONNX model loaded successfully. Inputs: {}, Outputs: {}",
                    session.getInputNames(), session.getOutputNames());
        } catch (Exception e) {
            log.error("Failed to load ONNX model from {}: {}", modelPath, e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize ONNX runtime: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void tearDown() {
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
        } catch (Exception e) {
            log.warn("Error closing ONNX environment: {}", e.getMessage());
        }
    }

    /**
     * Scores the transaction using the exact 6-element feature vector:
     * 1. so_giao_dich_5_phut (float)
     * 2. tong_tien_1_gio (float)
     * 3. trung_binh_lich_su (float)
     * 4. lech_so_voi_trung_binh (float)
     * 5. khoang_cach_bat_thuong (float: 0.0 or 1.0)
     * 6. amount (float)
     */
    public double scoreTransaction(FeatureDetails features, BigDecimal amount) {
        if (session == null) {
            throw new IllegalStateException("ONNX session is not initialized");
        }

        float[] featureVector = new float[]{
                (float) features.getSoGiaoDich5Phut(),
                features.getTongTien1Gio() != null ? features.getTongTien1Gio().floatValue() : 0.0f,
                features.getTrungBinhLichSu() != null ? features.getTrungBinhLichSu().floatValue() : 0.0f,
                features.getLechSoVoiTrungBinh() != null ? features.getLechSoVoiTrungBinh().floatValue() : 0.0f,
                features.isKhoangCachBatThuong() ? 1.0f : 0.0f,
                amount != null ? amount.floatValue() : 0.0f
        };

        try {
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(featureVector), new long[]{1, 6});
            String inputName = session.getInputNames().iterator().next();

            try (OrtSession.Result results = session.run(Collections.singletonMap(inputName, inputTensor))) {
                // The model exports probabilities as the second output (or named 'probabilities')
                // Output tensor is 2D float array of shape [1, 2]
                double fraudProbability = 0.0;
                for (Map.Entry<String, OnnxValue> entry : results) {
                    if ("probabilities".equals(entry.getKey()) || entry.getValue() instanceof OnnxTensor tensor) {
                        Object val = entry.getValue().getValue();
                        if (val instanceof float[][] probMatrix && probMatrix.length > 0 && probMatrix[0].length > 1) {
                            fraudProbability = probMatrix[0][1];
                            break;
                        }
                    }
                }

                // Bound to [0.0, 1.0]
                fraudProbability = Math.max(0.0, Math.min(1.0, fraudProbability));
                // Round to 4 decimal places
                return Math.round(fraudProbability * 10000.0) / 10000.0;
            } finally {
                inputTensor.close();
            }
        } catch (Exception e) {
            log.error("Failed to run ONNX model inference: {}", e.getMessage(), e);
            throw new IllegalStateException("Model inference failed: " + e.getMessage(), e);
        }
    }

    public String mapScoreToDecision(double riskScore) {
        MlThresholds thresholds = rulesLoader.getRulesConfig().getMlThresholds();
        double chanThreshold = thresholds != null ? thresholds.getChanNeuDiemTren() : 0.8;
        double xemXetThreshold = thresholds != null ? thresholds.getXemXetNeuDiemTren() : 0.4;

        if (riskScore > chanThreshold) {
            return "CHAN";
        } else if (riskScore > xemXetThreshold) {
            return "XEM_XET";
        } else {
            return "CHO_QUA";
        }
    }
}
