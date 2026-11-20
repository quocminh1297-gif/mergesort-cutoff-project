# Thực nghiệm tối ưu hóa ngưỡng CUTOFF cho Mergesort

Dự án thực nghiệm thuộc học phần **Cấu trúc dữ liệu và Giải thuật (DSA)** nhằm khảo sát, đo đạc và xác định giá trị `CUTOFF` (ngưỡng chuyển sang *Insertion Sort* cho mảng con kích thước nhỏ) tối ưu nhất đối với cài đặt *Mergesort* bám sát giáo trình *Algorithms, 4th Edition* (Robert Sedgewick & Kevin Wayne) trên cấu hình phần cứng thực tế — từ đó so sánh hai mốc khuyến nghị `7` (phiên bản cũ) và `10` (phiên bản mới).

---

## 1. Cấu trúc thư mục

```text
mergesort-cutoff-project/
├── src/
│   ├── Insertion.java          # Insertion sort trên toàn mảng và đoạn con a[lo..hi] theo bài 2.1
│   ├── Merge.java              # Top-down Mergesort thuần (baseline) và bản có CUTOFF theo bài 2.2
│   ├── SortTest.java           # Unit test kiểm tra tính đúng đắn, biên dữ liệu và tính ổn định (stability)
│   ├── DatasetGenerator.java   # Sinh và đọc bộ dữ liệu thực nghiệm cố định (seed = 2026)
│   └── Benchmark.java          # Chương trình đo hiệu năng (JVM warm-up, interleaved trials, thống kê CSV)
├── scripts/
│   └── plot_results.py         # Đọc file CSV và vẽ 4 biểu đồ PNG (300 DPI)
├── data/                       # Bộ dữ liệu thí nghiệm (sinh tự động với seed cố định)
├── results/
│   ├── environment.txt         # Thông số phần cứng, OS, phiên bản JDK và cờ JVM
│   ├── exp1_cutoff_raw.csv     # Dữ liệu thô từng lần chạy (Exp 1)
│   ├── exp1_cutoff_summary.csv # Bảng thống kê Exp 1 (5 phân bố, N = 1,000,000)
│   ├── exp2_datatype_summary.csv # Bảng thống kê Exp 2 (Integer vs String, N = 300,000)
│   ├── exp3_doubling_summary.csv # Bảng thống kê Exp 3 (Doubling Test, N = 125k -> 2M)
│   └── charts/                 # 4 biểu đồ kết quả thí nghiệm (.png)
├── report/                     # Báo cáo thí nghiệm (PDF / Markdown)
└── ai_logs/                    # Log trao đổi với AI trong quá trình thực hiện
```

---

## 2. Phạm vi cài đặt thuật toán

* **`Insertion.java`**: Cài đặt chuẩn theo slide *2.1 Elementary Sorts*, sử dụng `less()` và `exch()`, bổ sung hàm `sort(Comparable[] a, int lo, int hi)` để sắp xếp tại chỗ mảng con và bảo toàn tính ổn định.
* **`Merge.java`**: Cài đặt chuẩn theo slide *2.2 Mergesort*:
    * Cấp phát mảng phụ `aux[]` đúng một lần bên ngoài đệ quy.
    * Tách riêng `sortPure` (khi `cutoff <= 1`) làm mốc đối chứng (baseline) không chịu chi phí kiểm tra ngưỡng.
    * Cô lập duy nhất cải tiến `CUTOFF` (`if (hi <= lo + cutoff - 1) { Insertion.sort(a, lo, hi); return; }`), chưa gộp hai cải tiến phụ (*stop-if-already-sorted* và *eliminate copy to aux*) để đánh giá thuần tác động của `CUTOFF`.

---

## 3. Môi trường thực nghiệm

| Thông số | Giá trị |
| :--- | :--- |
| **CPU** | AMD Ryzen AI 9 365 w/ Radeon 880M (10 cores / 20 threads: 4 Zen 5 + 6 Zen 5c; Base 2.0 GHz, Max Boost 5.0 GHz) |
| **RAM** | 27.29 GB usable RAM |
| **Hệ điều hành** | Windows 11 10.0 (amd64), Power Mode: Best Performance |
| **JDK / JVM** | Oracle JDK 25.0.2 (Java HotSpot 64-Bit Server VM `25.0.2+10-LTS-69`) |
| **Cờ JVM** | `-Xms4g -Xmx4g -XX:+AlwaysPreTouch` |
| **Kiểm soát luồng** | Ghim cụm 4 nhân hiệu năng cao Zen 5 (`/HIGH /AFFINITY 0xFF`) để tránh dịch chuyển luồng sang nhân Zen 5c |

---

## 4. Hướng dẫn biên dịch và tái lập thí nghiệm

### Yêu cầu
* **Java**: JDK 17 trở lên (thí nghiệm gốc dùng JDK 25.0.2).
* **Python** (để vẽ đồ thị): Python 3.9+ cùng các thư viện `pandas`, `matplotlib`, `numpy`.

### Bước 1: Biên dịch và chạy Unit Test
```bash
javac -d out src/*.java
java -cp out SortTest
```
Kết quả mong đợi: `ALL TESTS PASSED!`

### Bước 2: Chạy Benchmark

* **Chế độ chạy nhanh (`--quick`)** để kiểm tra quy trình (~30 giây):
```bash
java -Xms4g -Xmx4g -XX:+AlwaysPreTouch -cp out Benchmark --quick
```

* **Chế độ đầy đủ (Full Mode — ~6–7 phút)** trên Windows PowerShell (ghim cụm nhân hiệu năng cao Zen 5):
```powershell
cmd /c "start /B /WAIT /HIGH /AFFINITY 0xFF java -Xms4g -Xmx4g -XX:+AlwaysPreTouch -cp out Benchmark"
```

* **Chạy chế độ đầy đủ trên macOS / Linux** (hoặc Windows không cần ghim nhân):
```bash
java -Xms4g -Xmx4g -XX:+AlwaysPreTouch -cp out Benchmark
```

### Bước 3: Vẽ đồ thị từ kết quả CSV
```bash
pip install pandas matplotlib numpy
python scripts/plot_results.py
```
4 biểu đồ độ phân giải cao (300 DPI) sẽ được xuất vào `results/charts/`:
1. `fig1_random_cutoff_curve.png` — Đường cong hình chữ U theo `CUTOFF` (toàn dải `1–128` và phóng to `1–32`).
2. `fig2_distributions_speedup.png` — Mức cải thiện (%) theo 5 phân bố dữ liệu.
3. `fig3_datatype_comparison.png` — So sánh ảnh hưởng của kiểu dữ liệu `Integer` và `String`.
4. `fig4_doubling_test.png` — Đồ thị Log-Log kiểm chứng bậc tăng trưởng và % cải thiện theo kích thước $N$.

---

## 5. Tóm tắt kết quả chính

### Thí nghiệm 1: Quét `CUTOFF` trên 5 phân bố dữ liệu (`Integer`, $N = 1{,}000{,}000$, 15 trials, thước đo Median)

| Phân bố dữ liệu | Pure (`C=1`) | `C=7` (Giáo trình cũ) | `C=10` (Giáo trình mới) | `CUTOFF` tốt nhất | Cải thiện tối đa |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **`random`** | 281.80 ms | 266.16 ms (`+5.55%`) | 268.20 ms (`+4.83%`) | **`12`** (262.61 ms) | **`+6.81%`** |
| **`partially_sorted`** | 201.73 ms | 179.91 ms (`+10.81%`) | 175.87 ms (`+12.82%`) | **`128`** (143.55 ms) | **`+28.84%`** |
| **`reverse`** | 198.87 ms | 193.03 ms (`+2.94%`) | 199.10 ms (`-0.12%`) | **`6`** (190.95 ms) | **`+3.98%`** |
| **`sorted`** | 197.87 ms | 172.62 ms (`+12.76%`) | 166.51 ms (`+15.85%`) | **`128`** (127.62 ms) | **`+35.51%`** |
| **`few_unique`** | 209.95 ms | 194.73 ms (`+7.25%`) | 195.29 ms (`+6.98%`) | **`12`** (194.67 ms) | **`+7.28%`** |

### Thí nghiệm 2 & 3: Ảnh hưởng kiểu dữ liệu và Doubling Test
* **Kiểu dữ liệu ($N = 300{,}000$, `random`)**: Cả `Integer` (tối ưu ở `C=8`, `+7.48%`) và `String` (tối ưu ở `C=9`, `+4.42%`) đều có vùng đáy phẳng trong khoảng `6–14`, nhưng mức cải thiện của `String` thấp hơn do chi phí hàm `String.compareTo()` chiếm tỷ trọng lớn.
* **Doubling Test ($N = 125\text{k} \to 2\text{M}$, 11 trials)**: Cả `C=1`, `C=7`, `C=10`, `C=12` đều giữ nguyên độ dốc log-log $b \approx 1.295$ (không đổi bậc tăng trưởng, chỉ giảm hệ số hằng). Mức cải thiện trung bình qua 5 cỡ $N$: `C=7` (`+4.80%`), `C=10` (`+5.48%`), `C=12` (`+5.54%`).

### Kết luận ngắn gọn (`7` vs `10`)
* Trên máy thử nghiệm và cài đặt bám sát bài giảng, tồn tại một **vùng tối ưu phẳng `CUTOFF = 7–12`** cho dữ liệu ngẫu nhiên (chênh lệch giữa `7`, `10`, `12` không có ý nghĩa thống kê đáng kể trên `random`, $p > 0.2$ ở kiểm định ghép cặp).
* **`CUTOFF = 7`** an toàn hơn trong trường hợp xấu nhất (`reverse` vẫn nhanh hơn `+2.94%`).
* **`CUTOFF = 10`** (hoặc **`10–12`**) là lựa chọn tổng quát nhỉnh hơn cho dữ liệu thực tế: đạt tốc độ trung bình tốt hơn qua các kích thước $N$ lớn trong Doubling Test và tận dụng tốt hơn tính tuyến tính của Insertion Sort trên dữ liệu gần như đã sắp xếp (`+12.82%` so với `+10.81%`).

---

## 6. Tài liệu tham khảo
* Robert Sedgewick & Kevin Wayne, *Algorithms, 4th Edition* — Chapter 1.3 (Analysis of Algorithms), 2.1 (Elementary Sorts), 2.2 (Mergesort).