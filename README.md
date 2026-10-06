# Real-Time Fraud Detection System (Hệ thống Phát hiện Gian lận Thời gian thực)

Hệ thống phát hiện gian lận thẻ tín dụng thời gian thực xây dựng trên nền tảng **Java 21**, **Spring Boot**, **Apache Kafka (KRaft)**, **Redis**, và **ONNX Runtime ML Engine**.

---

## 1. Tổng quan Dự án (Project Overview)

Trong các hệ thống tài chính và ngân hàng số, mỗi giao dịch thanh toán qua thẻ cần được thẩm định và đưa ra quyết định rủi ro trong vòng **vài chục mili-giây** trước khi tiền được chuyển đi:
- **`CHO_QUA` (Approve)**: Giao dịch an toàn, cho phép thực hiện.
- **`XEM_XET` (Review)**: Giao dịch có dấu hiệu bất thường, cần xác thực bổ sung (OTP/gọi điện).
- **`CHAN` (Block)**: Giao dịch gian lận rõ rệt hoặc vi phạm các quy tắc nghiệp vụ nghiêm ngặt, chặn ngay lập tức.

Hệ thống cung cấp:
1. **Simulator UI (`:8080`)**: Giao diện trực quan để gửi giao dịch thủ công, chạy 3 kịch bản gian lận dựng sẵn, và bật chế độ tự động sinh tải từ 1 đến 500 tx/s kèm Live Feed cập nhật thời gian thực.
2. **Feature Service (`:8083`)**: Xử lý luồng Kafka Streams tính toán 5 chỉ số đặc trưng (feature) theo cửa sổ trượt (sliding window) và lưu trữ vào Redis.
3. **Decision Service (`:8082`)**: Đánh giá theo thứ tự Rule YAML trước, nếu không vi phạm mới chuyển tiếp sang mô hình Machine Learning ONNX Runtime.
4. **Dashboard (`:8081`)**: Giám sát thời gian thực với biểu đồ thông lượng (TPS), tỷ lệ chặn (CHAN ratio), độ trễ (p50/p99) và bảng Top 10 giao dịch rủi ro cao nhất trong 1 giờ.
5. **Gatling Load Test**: Bộ công cụ đo hiệu năng tự động bắn trực tiếp vào Decision API để đo lường thông lượng thật và độ trễ p50/p99.

---

## 2. Kiến trúc Hệ thống (Architecture & Data Flow)

```
                  ┌────────────────────────────────────────────────────────┐
                  │                    Simulator UI                        │
                  │              (http://localhost:8080)                   │
                  └───────────────┬────────────────────────┬───────────────┘
                                  │                        │
                    1. Gửi event  │                        │ 2. Gọi Decision API
                       (Producer) │                        │    (Đo độ trễ thực)
                                  ▼                        ▼
                      ┌───────────────────────┐   ┌────────────────────────┐
                      │   Apache Kafka        │   │    Decision Service    │
                      │  Topic: transactions  │   │  POST /check-transaction│
                      └───────────┬───────────┘   └───────────┬────────────┘
                                  │                           │
                     3. Đọc luồng │                           │ 5. Đọc/Ghi state
                    (Kafka Streams)                           │    Redis Window
                                  ▼                           ▼
                      ┌───────────────────────┐   ┌────────────────────────┐
                      │    Feature Service    │──►│         Redis          │
                      │   (5 rolling features)│   │  features:<cardId>     │
                      └───────────────────────┘   │  tx:window:<cardId>    │
                                                  │  tx:data:<cardId>      │
                                                  └────────────────────────┘
                                                               ▲
                                                               │
                  ┌────────────────────────────────────────────┴───────────┐
                  │                 Monitoring Dashboard                   │
                  │              (http://localhost:8081)                   │
                  │  - Real-time TPS Trend  - Top 10 Risk (1 giờ)          │
                  │  - CHAN Ratio (%)       - Latency p50 & p99            │
                  └────────────────────────────────────────────────────────┘
```

---

## 3. Các Microservices & Cổng Dịch vụ (Services & Ports)

| Service | Port | Trách nhiệm |
|---|---|---|
| **tx-simulator** | `8080` | Giao diện mô phỏng Web, sinh giao dịch tự động/thủ công, kịch bản gian lận, đẩy Kafka event, gọi Decision API, Live Feed |
| **dashboard** | `8081` | Web UI giám sát chỉ số thời gian thực: TPS, CHAN %, p50/p99 latency, Top 10 giao dịch rủi ro cao nhất |
| **decision-service** | `8082` | Endpoint `POST /check-transaction`, đọc Redis window state, đánh giá YAML rules, chấm điểm ONNX model fallback |
| **feature-service** | `8083` | Kafka Streams streaming consumer, tính 5 đặc trưng theo cửa sổ trượt, cập nhật Redis |
| **kafka (KRaft)** | `9092` (host) / `29092` (mạng Docker) | Message broker phân tán, topic `transactions` với 3 partitions |
| **redis** | `6379` | In-memory Data Store cho sliding-window transactions và feature state |

---

## 4. Thiết kế 5 Đặc trưng (Feature Engineering)

| Tên chỉ số | Công thức / Ý nghĩa | Cửa sổ thời gian | Cơ chế xử lý |
|---|---|---|---|
| `so_giao_dich_5_phut` | Đếm số lượng giao dịch của thẻ trong 5 phút gần nhất | 5 phút trượt | Redis ZSET member theo timestamp |
| `tong_tien_1_gio` | Tổng số tiền chi tiêu của thẻ trong 1 giờ gần nhất | 1 giờ trượt | Redis ZSET range + HASH payload |
| `trung_binh_lich_su` | Trung bình chi tiêu 30 ngày (tính trước cho 20 thẻ demo) | 30 ngày (tĩnh) | Nạp sẵn lúc khởi động hệ thống |
| `lech_so_voi_trung_binh` | `(amount - trung_binh_lich_su) / trung_binh_lich_su` | Tại thời điểm GD | Tính tức thời so với lịch sử thẻ |
| `khoang_cach_bat_thuong` | Vận tốc di chuyển ngụ ý giữa 2 GD liên tiếp > 900 km/h | So với GD trước | Công thức Haversine chia cho $\Delta t$ |

---

## 5. Thiết kế Lưu trữ Redis (Redis State Design)

Để xử lý triệt để race condition khi có giao dịch dồn dập (rapid-fire) mà Kafka Streams chưa kịp consume:
1. **`tx:window:<cardId>`** (`ZSET`):
   - Member: `transactionId`
   - Score: `timestampEpochMs`
   - Tác dụng: Truy vấn các giao dịch trong cửa sổ 5 phút (`now - 300,000ms`) và 1 giờ (`now - 3,600,000ms`) bằng lệnh `ZREVRANGEBYSCORE`.
2. **`tx:data:<cardId>`** (`HASH`):
   - Field: `transactionId`
   - Value: Serialized JSON của bản ghi giao dịch (gồm `amount`, `location`, `timestamp`).
3. **`features:<cardId>`** (`HASH`):
   - Chứa snapshot các đặc trưng được tổng hợp sẵn từ luồng Kafka Streams.

---

## 6. Luồng Ra Quyết định (Decision Pipeline)

Mỗi giao dịch gửi đến `POST /check-transaction` trải qua 2 tầng đánh giá:

1. **Tầng 1: Rule Engine (`rules.yaml`)**:
   - Được nạp lúc khởi động (có thể cập nhật động không cần build lại code).
   - Đánh giá tuần tự từ trên xuống dưới:
     - `qua_nhieu_giao_dich`: `so_giao_dich_5_phut > 5` $\rightarrow$ **`CHAN`**
     - `di_chuyen_bat_kha_thi`: `khoang_cach_bat_thuong == true` $\rightarrow$ **`CHAN`**
     - `chi_tieu_qua_cao_tuyet_doi`: `amount > 50,000,000` $\rightarrow$ **`XEM_XET`**
   - **Quy tắc vàng**: Nếu có bất kỳ rule nào khớp, hệ thống quyết định ngay lập tức và **KHÔNG** gọi mô hình Machine Learning.
2. **Tầng 2: Machine Learning Fallback (`model.onnx`)**:
   - Khi không có rule nào vi phạm, vector đặc trưng 5 chiều được đưa vào mô hình ONNX Runtime (`float_input` $\rightarrow$ `probabilities`).
   - Ngưỡng quyết định rủi ro:
     - `riskScore > 0.8`: **`CHAN`**
     - `riskScore > 0.4`: **`XEM_XET`**
     - Còn lại: **`CHO_QUA`**

---

## 7. Hướng dẫn Khởi chạy Hệ thống (Quickstart)

### Cách 1: Khởi chạy toàn bộ bằng Docker Compose (Khuyến nghị)

Yêu cầu: Docker Desktop v24+ và Docker Compose v2+.

```bash
# 1. Build toàn bộ 4 microservices từ mã nguồn trong container
docker compose build

# 2. Khởi chạy toàn bộ 6 services ở chế độ nền
docker compose up -d

# 3. Kiểm tra trạng thái các containers
docker compose ps
```

Các dịch vụ sẽ sẵn sàng tại:
- **Simulator UI**: [http://localhost:8080](http://localhost:8080)
- **Monitoring Dashboard**: [http://localhost:8081](http://localhost:8081)
- **Decision API**: [http://localhost:8082](http://localhost:8082)
- **Feature Service**: [http://localhost:8083](http://localhost:8083)

Để dừng hệ thống:
```bash
docker compose down
```

---

### Cách 2: Khởi chạy cục bộ (Local Development)

Yêu cầu: Java 21 JDK, Docker (để chạy Kafka và Redis).

```bash
# 1. Bật Kafka và Redis nền
docker compose up -d kafka kafka-init redis

# 2. Chạy Decision Service (Port 8082)
.\mvnw.cmd spring-boot:run -pl decision-service

# 3. Chạy Feature Service (Port 8083)
.\mvnw.cmd spring-boot:run -pl feature-service

# 4. Chạy tx-simulator (Port 8080)
.\mvnw.cmd spring-boot:run -pl tx-simulator

# 5. Chạy dashboard (Port 8081)
.\mvnw.cmd spring-boot:run -pl dashboard
```

---

## 8. Hướng dẫn Sử dụng Simulator UI (`:8080`)

Mở trình duyệt tại [http://localhost:8080](http://localhost:8080):

### 8.1. Gửi giao dịch thủ công (Manual Transaction)
1. Chọn thẻ giả lập (`card-0001` đến `card-0020`).
2. Nhập số tiền (`amount`), merchant (Shopee, Grab, Circle K, ATM...) và thành phố (Hà Nội, TP.HCM, Đà Nẵng).
3. Bấm **"Gửi giao dịch"** $\rightarrow$ Kết quả (`CHO_QUA`, `XEM_XET`, hoặc `CHAN`), điểm rủi ro và rule vi phạm sẽ xuất hiện ngay lập tức.

### 8.2. Chạy 3 Kịch bản Gian lận Dựng sẵn (Preset Scenarios)
- **"Quẹt dồn dập" (Rapid-fire)**: Tự động gửi 10 giao dịch liên tiếp cho cùng 1 thẻ. Giao dịch thứ 6 trở đi sẽ bị **`CHAN`** ngay lập tức do vi phạm rule `qua_nhieu_giao_dich` (`so_giao_dich_5_phut > 5`).
- **"Di chuyển bất khả thi" (Impossible Travel)**: Gửi 1 giao dịch tại Hà Nội, sau đó 2 phút gửi tiếp tại TP.HCM (khoảng cách 1,160 km, ngụ ý vận tốc > 900 km/h). Giao dịch thứ 2 sẽ bị **`CHAN`** ngay lập tức do rule `di_chuyen_bat_kha_thi`.
- **"Chi tiêu bất thường" (Unusual Amount)**: Gửi giao dịch với số tiền gấp 20 lần mức trung bình lịch sử 30 ngày của thẻ $\rightarrow$ Đẩy sang **`XEM_XET`** hoặc **`CHAN`** dựa trên điểm rủi ro ML.

### 8.3. Chế độ Tự động (Auto Mode)
- Bật công tắc Auto Mode, điều chỉnh thanh slider từ **1 đến 500 tx/s**.
- Hệ thống tự sinh giao dịch ngẫu nhiên liên tục và hiển thị Live Feed theo thời gian thực.

---

## 9. Hướng dẫn Giám sát Dashboard (`:8081`)

Mở trình duyệt tại [http://localhost:8081](http://localhost:8081):
- **KPI Cards**: Hiển thị Tổng số giao dịch trong 1 giờ, Thông lượng hiện tại (TPS), Tỷ lệ chặn CHAN (%), Độ trễ trung vị p50 (ms), Độ trễ đuôi p99 (ms).
- **Biểu đồ thời gian thực (Real-time Charts)**:
  - TPS Trend (cửa sổ trượt 60 giây).
  - Tỷ lệ CHAN % theo thời gian.
  - Độ trễ xử lý p50 và p99 song hành.
- **Top 10 Giao dịch Rủi ro Cao nhất trong 1 giờ**: Bảng danh sách các giao dịch có điểm rủi ro cao nhất hoặc bị chặn bởi rule CHAN, cập nhật liên tục không cần tải lại trang.

---

## 10. Chạy Load Test bằng Gatling

Dự án đi kèm bộ test hiệu năng Gatling tại module `load-test`:

```bash
# Chạy Gatling load test trực tiếp vào Decision API (bỏ qua UI)
.\mvnw.cmd test-compile -f load-test/pom.xml
.\mvnw.cmd gatling:test -f load-test/pom.xml
```

Kịch bản kiểm thử:
- **Stage 1 (Warm-up / Baseline)**: 50 tx/s trong 15 giây
- **Stage 2 (Medium Load)**: 100 tx/s trong 15 giây
- **Stage 3 (High Load)**: 250 tx/s trong 15 giây
- **Stage 4 (Peak Load)**: 500 tx/s trong 15 giây

Báo cáo chi tiết và kết quả đo lường thực tế xem tại file [`LOAD_TEST.md`](./LOAD_TEST.md).

---

## 11. Kiểm thử Tự động & Build Verification

Chạy toàn bộ 57 unit tests và integration tests của hệ thống:

```bash
.\mvnw.cmd clean test
```

Kết quả:
- `common`: SUCCESS
- `tx-simulator`: 27 tests SUCCESS
- `feature-service`: 6 tests SUCCESS
- `decision-service`: 16 tests SUCCESS
- `dashboard`: 8 tests SUCCESS
- **BUILD SUCCESS**

---

## 12. Trade-offs & Thiết kế Kỹ thuật (Design Trade-offs)

1. **Gộp Decision API và Scoring Service**:
   - *Lý do*: Giảm bớt 1 chặng network hop (HTTP round-trip) không cần thiết, giúp giữ độ trễ trung vị p50 ở mức < 15ms.
2. **Polling 1s thay vì WebSocket/STOMP**:
   - *Lý do*: Đơn giản hóa kiến trúc frontend, không cần quản lý kết nối socket stateful, đủ mượt mà cho demo và dễ chịu lỗi khi container khởi động lại.
3. **In-memory Rolling Window Metrics trong Dashboard**:
   - *Lý do*: Không cần đưa thêm Elasticsearch, Prometheus hay InfluxDB cồng kềnh vào môi trường demo. Cửa sổ trượt 1 giờ được lưu trữ bằng cấu trúc dữ liệu RAM với thuật toán khử trùng lặp `transactionId` hiệu quả.
4. **Kafka KRaft Mode**:
   - *Lý do*: Bỏ hoàn toàn ZooKeeper, tiết kiệm tài nguyên RAM và đơn giản hóa việc cấu hình Docker Compose.

---

## 13. Ảnh chụp Màn hình Giao diện (Screenshots)

- **Simulator UI (`http://localhost:8080`)**: [`docs/screenshots/simulator-ui.png`](./docs/screenshots/simulator-ui.png)
- **Monitoring Dashboard (`http://localhost:8081`)**: [`docs/screenshots/dashboard.png`](./docs/screenshots/dashboard.png)

---

## 14. Hạn chế Đã biết (Known Limitations)

1. **Khởi động Kafka KRaft**: Cần đợi container Kafka healthy (~5-10s) trước khi `kafka-init` tạo topic.
2. **Khả năng chịu tải đỉnh 500 tx/s**: Với cấu hình đơn máy cục bộ, mức tải 500 tx/s đồng thời với ONNX Runtime inference và kết nối Redis đơn luồng có thể dẫn đến hiện tượng tăng độ trễ đuôi (p99) do cạnh tranh tài nguyên CPU.
