# Model Training: Real-Time Card Transaction Fraud Risk Scoring

## Overview
This module trains a machine learning model (`RandomForestClassifier`) to predict the probability of transaction fraud based on 6 core features, and exports it to ONNX format (`model.onnx`) for high-performance Java inference via ONNX Runtime.

## Feature Vector (Strict Order)
Both Python training and Java inference must use this exact sequence of 6 features:
1. `so_giao_dich_5_phut` (float): Count of transactions for this card in the 5-minute sliding window.
2. `tong_tien_1_gio` (float): Total spending for this card in the 1-hour sliding window.
3. `trung_binh_lich_su` (float): 30-day baseline historical average amount for this card.
4. `lech_so_voi_trung_binh` (float): Relative deviation `(amount - baseline) / baseline`.
5. `khoang_cach_bat_thuong` (float): 1.0 if implied speed from previous transaction > 900 km/h, else 0.0.
6. `amount` (float): Amount of the current transaction.

## How to Train & Export
1. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
2. Run training:
   ```bash
   python train.py
   ```
3. The script will generate `model.onnx` in this directory and verify predictions against normal and suspicious samples.
4. The generated `model.onnx` is copied to `decision-service/src/main/resources/model.onnx` for Java runtime inference.
