import numpy as np
import os
from sklearn.ensemble import RandomForestClassifier
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType
import onnxruntime as ort

def generate_synthetic_data(num_samples=12000, random_seed=42):
    np.random.seed(random_seed)
    
    # 85% normal, 15% fraud
    num_fraud = int(num_samples * 0.15)
    num_normal = num_samples - num_fraud
    
    # Normal transactions:
    # 1. so_giao_dich_5_phut: 1 to 3
    normal_count_5m = np.random.choice([1, 2, 3], size=num_normal, p=[0.7, 0.25, 0.05])
    # 3. trung_binh_lich_su: 200,000 to 2,000,000 VND
    normal_baseline = np.random.uniform(200_000, 2_000_000, size=num_normal)
    # 4. lech_so_voi_trung_binh: -0.6 to +0.8
    normal_deviation = np.random.uniform(-0.6, 0.8, size=num_normal)
    # 6. amount = baseline * (1 + deviation)
    normal_amount = normal_baseline * (1.0 + normal_deviation)
    # 2. tong_tien_1_gio = amount * count_5m roughly
    normal_sum_1h = normal_amount * normal_count_5m * np.random.uniform(0.9, 1.3, size=num_normal)
    # 5. khoang_cach_bat_thuong: 0.0
    normal_speed_flag = np.zeros(num_normal, dtype=np.float32)
    
    X_normal = np.column_stack([
        normal_count_5m.astype(np.float32),
        normal_sum_1h.astype(np.float32),
        normal_baseline.astype(np.float32),
        normal_deviation.astype(np.float32),
        normal_speed_flag,
        normal_amount.astype(np.float32)
    ])
    y_normal = np.zeros(num_normal, dtype=np.int64)
    
    # Fraud transactions:
    # High burst or high deviation or impossible travel
    fraud_count_5m = np.random.choice([1, 2, 4, 5, 6, 8], size=num_fraud, p=[0.1, 0.1, 0.2, 0.2, 0.2, 0.2])
    fraud_baseline = np.random.uniform(200_000, 2_000_000, size=num_fraud)
    fraud_deviation = np.random.uniform(2.5, 15.0, size=num_fraud)
    fraud_amount = fraud_baseline * (1.0 + fraud_deviation)
    fraud_sum_1h = fraud_amount * fraud_count_5m * np.random.uniform(1.0, 2.0, size=num_fraud)
    fraud_speed_flag = np.random.choice([0.0, 1.0], size=num_fraud, p=[0.6, 0.4]).astype(np.float32)
    
    X_fraud = np.column_stack([
        fraud_count_5m.astype(np.float32),
        fraud_sum_1h.astype(np.float32),
        fraud_baseline.astype(np.float32),
        fraud_deviation.astype(np.float32),
        fraud_speed_flag,
        fraud_amount.astype(np.float32)
    ])
    y_fraud = np.ones(num_fraud, dtype=np.int64)
    
    X = np.vstack([X_normal, X_fraud]).astype(np.float32)
    y = np.concatenate([y_normal, y_fraud])
    
    # Shuffle
    indices = np.arange(len(y))
    np.random.shuffle(indices)
    return X[indices], y[indices]

def train_and_export():
    print("Generating synthetic transaction dataset...")
    X, y = generate_synthetic_data(num_samples=12000, random_seed=42)
    print(f"Dataset shape: X={X.shape}, y={y.shape} (Fraud ratio: {np.mean(y):.2%})")
    
    print("Training RandomForestClassifier...")
    clf = RandomForestClassifier(n_estimators=40, max_depth=5, random_state=42)
    clf.fit(X, y)
    train_acc = clf.score(X, y)
    print(f"Model trained. Accuracy on training set: {train_acc:.4f}")
    
    # Export to ONNX
    print("Converting model to ONNX...")
    initial_type = [('float_input', FloatTensorType([None, 6]))]
    # zipmap=False exports probabilities directly as a 2D float tensor [N, 2]
    onnx_model = convert_sklearn(clf, initial_types=initial_type, target_opset=20, options={id(clf): {'zipmap': False}})
    
    output_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(output_dir, "model.onnx")
    with open(model_path, "wb") as f:
        f.write(onnx_model.SerializeToString())
    print(f"ONNX model exported to: {model_path}")
    
    # Verify with onnxruntime
    print("Verifying ONNX model inference with onnxruntime...")
    session = ort.InferenceSession(model_path)
    input_name = session.get_inputs()[0].name
    
    # Test 1: Normal transaction
    # [count_5m=1, sum_1h=350k, baseline=350k, deviation=0.0, speed_flag=0.0, amount=350k]
    sample_normal = np.array([[1.0, 350000.0, 350000.0, 0.0, 0.0, 350000.0]], dtype=np.float32)
    outputs_normal = session.run(None, {input_name: sample_normal})
    prob_normal = outputs_normal[1][0][1]
    print(f"Normal transaction test risk score: {prob_normal:.4f} (Expected <= 0.40)")
    
    # Test 2: High risk transaction
    # [count_5m=4, sum_1h=30M, baseline=500k, deviation=7.0, speed_flag=0.0, amount=4M]
    sample_fraud = np.array([[4.0, 30_000_000.0, 500_000.0, 7.0, 0.0, 4_000_000.0]], dtype=np.float32)
    outputs_fraud = session.run(None, {input_name: sample_fraud})
    prob_fraud = outputs_fraud[1][0][1]
    print(f"Suspicious transaction test risk score: {prob_fraud:.4f} (Expected >= 0.40)")
    
    print("Model training and verification completed successfully!")

if __name__ == "__main__":
    train_and_export()
